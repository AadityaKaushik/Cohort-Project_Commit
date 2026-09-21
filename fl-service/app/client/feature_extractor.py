"""Local, point-in-time-correct feature extraction (US-58, P44).

Turns one client's own raw records into the 24-feature `FeatureVector`
schema v1 defines (`app/common/schema.py`). Nothing here reads another
client's data -- per §22.3, each FL client's feature extractor reads only
its own schema -- and this module has no database dependency at all: it is
handed plain, already-local Python records (the Java-side sanitised export
job, P51, is what will populate them from a real client's Postgres schema;
that wiring is explicitly out of scope for P43-P49).

**Point-in-time correctness is the whole point of this module.** Every
computed feature must reflect only what the client knew *as of* a given
cutoff (`as_of`) -- never anything that happened after it. This matters for
two reasons: (1) training on a feature that "knows the future" relative to
its own label is a correctness bug (temporal leakage), not just a privacy
one; (2) it is also the property under adversarial test in
`test_feature_extractor.py` -- a record timestamped even one second after
`as_of` must be excluded, not merely "usually filtered".

To keep that property auditable in one place, every raw collection is
funnelled through `_before_cutoff` before any feature touches it.
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import datetime
from typing import Callable, Iterable, TypeVar

from app.common.schema import FeatureVector

# PROJECT_PLAN.md §17.2 / Interview.java's InterviewRoundType enum order --
# round_type_ordinal's encoding is this list's index, normalised to [0,1].
ROUND_TYPE_ORDER: tuple[str, ...] = (
    "OA",
    "PHONE_SCREEN",
    "TECHNICAL",
    "SYSTEM_DESIGN",
    "BEHAVIOURAL",
    "HR",
    "FINAL",
)

# §22.2 count-like feature denominators -- chosen to match the plan's own
# worked examples (log1p(137)/log1p(300) for application_volume, etc.).
_MASTERY_DECAY_HALF_LIFE_DAYS = 30.0
_RECENCY_DECAY_DAYS = 30.0
_CONSISTENCY_WINDOW_WEEKS = 8
_CONFIDENCE_WINDOW = 10
_TREND_WINDOW = 20
_SELF_RATING_WINDOW = 5
_PREP_LEAD_TIME_CAP_DAYS = 60.0
_ROUNDS_PER_APPLICATION_CAP = 6.0
_RESUME_VARIANT_CAP = 5
_DEFAULT_TAXONOMY_SIZE = 40

T = TypeVar("T")


@dataclass(frozen=True)
class PracticeAttemptRecord:
    topic: str
    category: str  # "dsa" | "technical" | "behavioural" | "system_design"
    difficulty: str  # "easy" | "medium" | "hard"
    confidence: float  # raw self-reported confidence, 0..10 (PracticeAttempt.confidence's scale)
    occurred_at: datetime


@dataclass(frozen=True)
class TaskRecord:
    created_at: datetime
    completed_at: datetime | None = None


@dataclass(frozen=True)
class ApplicationRecord:
    application_id: str
    created_at: datetime


@dataclass(frozen=True)
class ResumeVariantRecord:
    created_at: datetime


@dataclass(frozen=True)
class InterviewRoundRecord:
    """One *historical* round. The round being featurized (the label's
    subject) is passed separately as `target_round` -- it is never included
    here, so it can't accidentally count itself as "prior experience"."""

    application_id: str
    round_type: str
    topics_tested: tuple[str, ...]
    self_rating: float | None  # 1..5 self-reported (InterviewFeedback.selfRating's scale)
    occurred_at: datetime


@dataclass(frozen=True)
class TargetRound:
    """The round a feature vector is being computed *for*. Its own topics
    and type are known facts about the round itself, not historical
    evidence subject to the `as_of` cutoff -- but everything it is compared
    against (past practice, past rounds) still is."""

    round_type: str
    topics_tested: tuple[str, ...]


@dataclass(frozen=True)
class ClientDataset:
    """Everything one FL client can see: its own user's raw history, and
    nothing else. Unfiltered on purpose -- `extract_features` is the single
    place the `as_of` cutoff is applied, so that property has one place to
    get right (and one place to test)."""

    practice_attempts: tuple[PracticeAttemptRecord, ...] = ()
    tasks: tuple[TaskRecord, ...] = ()
    applications: tuple[ApplicationRecord, ...] = ()
    resume_variants: tuple[ResumeVariantRecord, ...] = ()
    interview_rounds: tuple[InterviewRoundRecord, ...] = ()


def _before_cutoff(items: Iterable[T], as_of: datetime, timestamp: Callable[[T], datetime | None]) -> list[T]:
    """The single choke point every record passes through before it can
    influence a feature. Strictly `<= as_of`; a record with no timestamp
    (e.g. an uncompleted task) never passes."""
    kept = []
    for item in items:
        ts = timestamp(item)
        if ts is not None and ts <= as_of:
            kept.append(item)
    return kept


def _clip(value: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, value))


def _mastery_by_topic(attempts: list[PracticeAttemptRecord], as_of: datetime) -> dict[str, float]:
    by_topic: dict[str, list[PracticeAttemptRecord]] = {}
    for attempt in attempts:
        by_topic.setdefault(attempt.topic, []).append(attempt)

    mastery: dict[str, float] = {}
    for topic, topic_attempts in by_topic.items():
        weights = []
        values = []
        for attempt in topic_attempts:
            days_ago = (as_of - attempt.occurred_at).total_seconds() / 86400.0
            weights.append(math.exp(-days_ago / _MASTERY_DECAY_HALF_LIFE_DAYS))
            values.append(_clip(attempt.confidence / 10.0))
        total_weight = sum(weights)
        mastery[topic] = sum(w * v for w, v in zip(weights, values)) / total_weight if total_weight > 0 else 0.0
    return mastery


def extract_features(
    dataset: ClientDataset,
    as_of: datetime,
    target_round: TargetRound,
    taxonomy_size: int = _DEFAULT_TAXONOMY_SIZE,
) -> FeatureVector:
    """The one entry point: raw local records + a point-in-time cutoff +
    the round being featurized -> the 24-feature schema v1 vector.

    Every collection below is passed through `_before_cutoff` first. No
    feature computation ever touches `dataset.*` directly.
    """
    attempts = _before_cutoff(dataset.practice_attempts, as_of, lambda a: a.occurred_at)
    tasks_created = _before_cutoff(dataset.tasks, as_of, lambda t: t.created_at)
    tasks_completed = _before_cutoff(dataset.tasks, as_of, lambda t: t.completed_at)
    applications = _before_cutoff(dataset.applications, as_of, lambda a: a.created_at)
    resume_variants = _before_cutoff(dataset.resume_variants, as_of, lambda r: r.created_at)
    rounds = _before_cutoff(dataset.interview_rounds, as_of, lambda r: r.occurred_at)

    # --- mastery (1-3) ---
    mastery = _mastery_by_topic(attempts, as_of)
    mastery_values = list(mastery.values())
    if mastery_values:
        mastery_mean = sum(mastery_values) / len(mastery_values)
        mastery_min = min(mastery_values)
        variance = sum((v - mastery_mean) ** 2 for v in mastery_values) / len(mastery_values)
        mastery_std = _clip(math.sqrt(variance))
    else:
        mastery_mean = mastery_min = mastery_std = 0.0

    # --- practice_volume (4) ---
    practice_volume = _clip(math.log1p(len(attempts)) / math.log1p(500))

    # --- topic_breadth (5) ---
    distinct_topics = len({a.topic for a in attempts})
    topic_breadth = _clip(distinct_topics / taxonomy_size) if taxonomy_size > 0 else 0.0

    # --- practice_recency (6) ---
    if attempts:
        last_attempt = max(a.occurred_at for a in attempts)
        days_since_last = (as_of - last_attempt).total_seconds() / 86400.0
        practice_recency = _clip(math.exp(-days_since_last / _RECENCY_DECAY_DAYS))
    else:
        practice_recency = 0.0

    # --- practice_consistency (7): fraction of the last 8 weeks with >=1 attempt ---
    weeks_with_activity = 0
    for week in range(_CONSISTENCY_WINDOW_WEEKS):
        window_end = as_of.timestamp() - week * 7 * 86400
        window_start = window_end - 7 * 86400
        if any(window_start < a.occurred_at.timestamp() <= window_end for a in attempts):
            weeks_with_activity += 1
    practice_consistency = weeks_with_activity / _CONSISTENCY_WINDOW_WEEKS

    # --- confidence_mean (8): mean confidence over the last 10 attempts ---
    recent_attempts = sorted(attempts, key=lambda a: a.occurred_at)[-_CONFIDENCE_WINDOW:]
    if recent_attempts:
        confidence_mean = _clip(sum(a.confidence for a in recent_attempts) / len(recent_attempts) / 10.0)
    else:
        confidence_mean = 0.0

    # --- confidence_trend (9): clipped slope over the last 20 attempts ---
    trend_attempts = sorted(attempts, key=lambda a: a.occurred_at)[-_TREND_WINDOW:]
    if len(trend_attempts) >= 2:
        xs = list(range(len(trend_attempts)))
        ys = [a.confidence / 10.0 for a in trend_attempts]
        mean_x = sum(xs) / len(xs)
        mean_y = sum(ys) / len(ys)
        numerator = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
        denominator = sum((x - mean_x) ** 2 for x in xs)
        slope = numerator / denominator if denominator > 0 else 0.0
        # Total predicted change across the window, clipped to [-1,1].
        confidence_trend = max(-1.0, min(1.0, slope * (len(trend_attempts) - 1)))
    else:
        confidence_trend = 0.0

    # --- hard_ratio (10) ---
    hard_ratio = sum(1 for a in attempts if a.difficulty == "hard") / len(attempts) if attempts else 0.0

    # --- category shares (11-14) ---
    def share(category: str) -> float:
        return sum(1 for a in attempts if a.category == category) / len(attempts) if attempts else 0.0

    share_dsa = share("dsa")
    share_technical = share("technical")
    share_behavioural = share("behavioural")
    share_system_design = share("system_design")

    # --- task_completion_rate (15) ---
    task_completion_rate = len(tasks_completed) / len(tasks_created) if tasks_created else 0.0

    # --- prep_lead_time (16) ---
    if attempts:
        earliest = min(a.occurred_at for a in attempts)
        lead_days = max(0.0, (as_of - earliest).total_seconds() / 86400.0)
        prep_lead_time = _clip(min(lead_days, _PREP_LEAD_TIME_CAP_DAYS) / _PREP_LEAD_TIME_CAP_DAYS)
    else:
        prep_lead_time = 0.0

    # --- interview_experience (17) ---
    interview_experience = _clip(math.log1p(len(rounds)) / math.log1p(50))

    # --- rounds_per_application (18) ---
    if rounds:
        counts_by_application: dict[str, int] = {}
        for r in rounds:
            counts_by_application[r.application_id] = counts_by_application.get(r.application_id, 0) + 1
        mean_rounds = sum(counts_by_application.values()) / len(counts_by_application)
        rounds_per_application = _clip(mean_rounds / _ROUNDS_PER_APPLICATION_CAP)
    else:
        rounds_per_application = 0.0

    # --- recent_self_rating (19) ---
    rated_rounds = [r for r in sorted(rounds, key=lambda r: r.occurred_at) if r.self_rating is not None]
    recent_rated = rated_rounds[-_SELF_RATING_WINDOW:]
    if recent_rated:
        mean_rating = sum(r.self_rating for r in recent_rated) / len(recent_rated)  # type: ignore[misc]
        recent_self_rating = _clip((mean_rating - 1.0) / 4.0)  # 1..5 -> [0,1]
    else:
        recent_self_rating = 0.0

    # --- coverage_ratio (20) ---
    practiced_topics = {a.topic for a in attempts}
    target_topics = set(target_round.topics_tested)
    coverage_ratio = (
        len(target_topics & practiced_topics) / len(target_topics) if target_topics else 0.0
    )

    # --- repeat_topic_ratio (21) ---
    prior_round_topics: set[str] = set()
    for r in rounds:
        prior_round_topics.update(r.topics_tested)
    repeat_topic_ratio = (
        len(target_topics & prior_round_topics) / len(target_topics) if target_topics else 0.0
    )

    # --- application_volume (22) ---
    application_volume = _clip(math.log1p(len(applications)) / math.log1p(300))

    # --- resume_variants (23) ---
    resume_variants_feature = min(len(resume_variants), _RESUME_VARIANT_CAP) / _RESUME_VARIANT_CAP

    # --- round_type_ordinal (24) ---
    try:
        index = ROUND_TYPE_ORDER.index(target_round.round_type)
    except ValueError as exc:
        raise ValueError(f"unknown round_type {target_round.round_type!r}") from exc
    round_type_ordinal = index / (len(ROUND_TYPE_ORDER) - 1)

    return FeatureVector(
        mastery_mean=mastery_mean,
        mastery_min=mastery_min,
        mastery_std=mastery_std,
        practice_volume=practice_volume,
        topic_breadth=topic_breadth,
        practice_recency=practice_recency,
        practice_consistency=practice_consistency,
        confidence_mean=confidence_mean,
        confidence_trend=confidence_trend,
        hard_ratio=hard_ratio,
        share_dsa=share_dsa,
        share_technical=share_technical,
        share_behavioural=share_behavioural,
        share_system_design=share_system_design,
        task_completion_rate=task_completion_rate,
        prep_lead_time=prep_lead_time,
        interview_experience=interview_experience,
        rounds_per_application=rounds_per_application,
        recent_self_rating=recent_self_rating,
        coverage_ratio=coverage_ratio,
        repeat_topic_ratio=repeat_topic_ratio,
        application_volume=application_volume,
        resume_variants=resume_variants_feature,
        round_type_ordinal=round_type_ordinal,
    )
