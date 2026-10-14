from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import datetime
from typing import Callable, Iterable, TypeVar

from app.common.schema import FeatureVector

ROUND_TYPE_ORDER: tuple[str, ...] = (
    "OA",
    "PHONE_SCREEN",
    "TECHNICAL",
    "SYSTEM_DESIGN",
    "BEHAVIOURAL",
    "HR",
    "FINAL",
)

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
    """One *historical* round."""

    application_id: str
    round_type: str
    topics_tested: tuple[str, ...]
    self_rating: float | None  # 1..5 self-reported (InterviewFeedback.selfRating's scale)
    occurred_at: datetime

@dataclass(frozen=True)
class TargetRound:
    round_type: str
    topics_tested: tuple[str, ...]

@dataclass(frozen=True)
class ClientDataset:
    practice_attempts: tuple[PracticeAttemptRecord, ...] = ()
    tasks: tuple[TaskRecord, ...] = ()
    applications: tuple[ApplicationRecord, ...] = ()
    resume_variants: tuple[ResumeVariantRecord, ...] = ()
    interview_rounds: tuple[InterviewRoundRecord, ...] = ()

def _before_cutoff(items: Iterable[T], as_of: datetime, timestamp: Callable[[T], datetime | None]) -> list[T]:
    """The single choke point every record passes through before it can influence a feature."""
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
    attempts = _before_cutoff(dataset.practice_attempts, as_of, lambda a: a.occurred_at)
    tasks_created = _before_cutoff(dataset.tasks, as_of, lambda t: t.created_at)
    tasks_completed = _before_cutoff(dataset.tasks, as_of, lambda t: t.completed_at)
    applications = _before_cutoff(dataset.applications, as_of, lambda a: a.created_at)
    resume_variants = _before_cutoff(dataset.resume_variants, as_of, lambda r: r.created_at)
    rounds = _before_cutoff(dataset.interview_rounds, as_of, lambda r: r.occurred_at)

    mastery = _mastery_by_topic(attempts, as_of)
    mastery_values = list(mastery.values())
    if mastery_values:
        mastery_mean = sum(mastery_values) / len(mastery_values)
        mastery_min = min(mastery_values)
        variance = sum((v - mastery_mean) ** 2 for v in mastery_values) / len(mastery_values)
        mastery_std = _clip(math.sqrt(variance))
    else:
        mastery_mean = mastery_min = mastery_std = 0.0

    practice_volume = _clip(math.log1p(len(attempts)) / math.log1p(500))

    distinct_topics = len({a.topic for a in attempts})
    topic_breadth = _clip(distinct_topics / taxonomy_size) if taxonomy_size > 0 else 0.0

    if attempts:
        last_attempt = max(a.occurred_at for a in attempts)
        days_since_last = (as_of - last_attempt).total_seconds() / 86400.0
        practice_recency = _clip(math.exp(-days_since_last / _RECENCY_DECAY_DAYS))
    else:
        practice_recency = 0.0

    weeks_with_activity = 0
    for week in range(_CONSISTENCY_WINDOW_WEEKS):
        window_end = as_of.timestamp() - week * 7 * 86400
        window_start = window_end - 7 * 86400
        if any(window_start < a.occurred_at.timestamp() <= window_end for a in attempts):
            weeks_with_activity += 1
    practice_consistency = weeks_with_activity / _CONSISTENCY_WINDOW_WEEKS

    recent_attempts = sorted(attempts, key=lambda a: a.occurred_at)[-_CONFIDENCE_WINDOW:]
    if recent_attempts:
        confidence_mean = _clip(sum(a.confidence for a in recent_attempts) / len(recent_attempts) / 10.0)
    else:
        confidence_mean = 0.0

    trend_attempts = sorted(attempts, key=lambda a: a.occurred_at)[-_TREND_WINDOW:]
    if len(trend_attempts) >= 2:
        xs = list(range(len(trend_attempts)))
        ys = [a.confidence / 10.0 for a in trend_attempts]
        mean_x = sum(xs) / len(xs)
        mean_y = sum(ys) / len(ys)
        numerator = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
        denominator = sum((x - mean_x) ** 2 for x in xs)
        slope = numerator / denominator if denominator > 0 else 0.0
        confidence_trend = max(-1.0, min(1.0, slope * (len(trend_attempts) - 1)))
    else:
        confidence_trend = 0.0

    hard_ratio = sum(1 for a in attempts if a.difficulty == "hard") / len(attempts) if attempts else 0.0

    def share(category: str) -> float:
        return sum(1 for a in attempts if a.category == category) / len(attempts) if attempts else 0.0

    share_dsa = share("dsa")
    share_technical = share("technical")
    share_behavioural = share("behavioural")
    share_system_design = share("system_design")

    task_completion_rate = len(tasks_completed) / len(tasks_created) if tasks_created else 0.0

    if attempts:
        earliest = min(a.occurred_at for a in attempts)
        lead_days = max(0.0, (as_of - earliest).total_seconds() / 86400.0)
        prep_lead_time = _clip(min(lead_days, _PREP_LEAD_TIME_CAP_DAYS) / _PREP_LEAD_TIME_CAP_DAYS)
    else:
        prep_lead_time = 0.0

    interview_experience = _clip(math.log1p(len(rounds)) / math.log1p(50))

    if rounds:
        counts_by_application: dict[str, int] = {}
        for r in rounds:
            counts_by_application[r.application_id] = counts_by_application.get(r.application_id, 0) + 1
        mean_rounds = sum(counts_by_application.values()) / len(counts_by_application)
        rounds_per_application = _clip(mean_rounds / _ROUNDS_PER_APPLICATION_CAP)
    else:
        rounds_per_application = 0.0

    rated_rounds = [r for r in sorted(rounds, key=lambda r: r.occurred_at) if r.self_rating is not None]
    recent_rated = rated_rounds[-_SELF_RATING_WINDOW:]
    if recent_rated:
        mean_rating = sum(r.self_rating for r in recent_rated) / len(recent_rated)  # type: ignore[misc]
        recent_self_rating = _clip((mean_rating - 1.0) / 4.0)  # 1..5 -> [0,1]
    else:
        recent_self_rating = 0.0

    practiced_topics = {a.topic for a in attempts}
    target_topics = set(target_round.topics_tested)
    coverage_ratio = (
        len(target_topics & practiced_topics) / len(target_topics) if target_topics else 0.0
    )

    prior_round_topics: set[str] = set()
    for r in rounds:
        prior_round_topics.update(r.topics_tested)
    repeat_topic_ratio = (
        len(target_topics & prior_round_topics) / len(target_topics) if target_topics else 0.0
    )

    application_volume = _clip(math.log1p(len(applications)) / math.log1p(300))

    resume_variants_feature = min(len(resume_variants), _RESUME_VARIANT_CAP) / _RESUME_VARIANT_CAP

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
