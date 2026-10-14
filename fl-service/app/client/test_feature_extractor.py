from __future__ import annotations

from dataclasses import replace
from datetime import datetime, timedelta, timezone

import pytest

from app.client.feature_extractor import (
    ApplicationRecord,
    ClientDataset,
    InterviewRoundRecord,
    PracticeAttemptRecord,
    ResumeVariantRecord,
    TargetRound,
    TaskRecord,
    extract_features,
)

AS_OF = datetime(2026, 1, 15, 12, 0, 0, tzinfo=timezone.utc)
ONE_SECOND = timedelta(seconds=1)

TARGET = TargetRound(round_type="TECHNICAL", topics_tested=("dynamic_programming", "graphs"))

def _baseline_dataset() -> ClientDataset:
    return ClientDataset(
        practice_attempts=(
            PracticeAttemptRecord(
                topic="arrays",
                category="dsa",
                difficulty="medium",
                confidence=6.0,
                occurred_at=AS_OF - timedelta(days=5),
            ),
            PracticeAttemptRecord(
                topic="graphs",
                category="dsa",
                difficulty="hard",
                confidence=4.0,
                occurred_at=AS_OF - timedelta(days=2),
            ),
        ),
        tasks=(
            TaskRecord(created_at=AS_OF - timedelta(days=10), completed_at=AS_OF - timedelta(days=8)),
        ),
        applications=(ApplicationRecord(application_id="app-1", created_at=AS_OF - timedelta(days=20)),),
        resume_variants=(ResumeVariantRecord(created_at=AS_OF - timedelta(days=30)),),
        interview_rounds=(
            InterviewRoundRecord(
                application_id="app-1",
                round_type="PHONE_SCREEN",
                topics_tested=("arrays",),
                self_rating=4.0,
                occurred_at=AS_OF - timedelta(days=15),
            ),
        ),
    )

def test_a_practice_attempt_one_second_after_as_of_does_not_leak() -> None:
    baseline = _baseline_dataset()
    leaky_attempt = PracticeAttemptRecord(
        topic="system_design_secret",
        category="system_design",
        difficulty="hard",
        confidence=10.0,  # deliberately extreme, so any leak is impossible to miss
        occurred_at=AS_OF + ONE_SECOND,
    )
    leaky = replace(baseline, practice_attempts=baseline.practice_attempts + (leaky_attempt,))

    baseline_vector = extract_features(baseline, AS_OF, TARGET)
    leaky_vector = extract_features(leaky, AS_OF, TARGET)

    assert leaky_vector == baseline_vector

def test_a_task_completed_one_second_after_as_of_does_not_count() -> None:
    baseline = _baseline_dataset()
    leaky_task = TaskRecord(
        created_at=AS_OF - timedelta(days=1),
        completed_at=AS_OF + ONE_SECOND,
    )
    leaky = replace(baseline, tasks=baseline.tasks + (leaky_task,))

    baseline_vector = extract_features(baseline, AS_OF, TARGET)
    leaky_vector = extract_features(leaky, AS_OF, TARGET)

    assert leaky_vector.task_completion_rate < baseline_vector.task_completion_rate
    assert leaky_vector.task_completion_rate == pytest.approx(1 / 2)

def test_an_application_created_one_second_after_as_of_does_not_leak() -> None:
    baseline = _baseline_dataset()
    leaky_application = ApplicationRecord(application_id="app-future", created_at=AS_OF + ONE_SECOND)
    leaky = replace(baseline, applications=baseline.applications + (leaky_application,))

    assert extract_features(leaky, AS_OF, TARGET) == extract_features(baseline, AS_OF, TARGET)

def test_a_resume_variant_created_one_second_after_as_of_does_not_leak() -> None:
    baseline = _baseline_dataset()
    leaky_variant = ResumeVariantRecord(created_at=AS_OF + ONE_SECOND)
    leaky = replace(baseline, resume_variants=baseline.resume_variants + (leaky_variant,))

    assert extract_features(leaky, AS_OF, TARGET) == extract_features(baseline, AS_OF, TARGET)

def test_an_interview_round_one_second_after_as_of_does_not_leak() -> None:
    baseline = _baseline_dataset()
    leaky_round = InterviewRoundRecord(
        application_id="app-1",
        round_type="FINAL",
        topics_tested=("dynamic_programming",),  # overlaps TARGET's topics
        self_rating=1.0,  # deliberately extreme
        occurred_at=AS_OF + ONE_SECOND,
    )
    leaky = replace(baseline, interview_rounds=baseline.interview_rounds + (leaky_round,))

    baseline_vector = extract_features(baseline, AS_OF, TARGET)
    leaky_vector = extract_features(leaky, AS_OF, TARGET)

    assert leaky_vector == baseline_vector
    # In particular, the leaky round's topic overlap with TARGET must not
    # inflate repeat_topic_ratio -- the most tempting place for this bug to hide.
    assert leaky_vector.repeat_topic_ratio == baseline_vector.repeat_topic_ratio

def test_a_record_exactly_at_as_of_is_included_not_excluded() -> None:
    baseline = _baseline_dataset()
    boundary_attempt = PracticeAttemptRecord(
        topic="boundary_topic", category="dsa", difficulty="easy", confidence=5.0, occurred_at=AS_OF
    )
    with_boundary = replace(baseline, practice_attempts=baseline.practice_attempts + (boundary_attempt,))

    baseline_vector = extract_features(baseline, AS_OF, TARGET)
    boundary_vector = extract_features(with_boundary, AS_OF, TARGET)

    assert boundary_vector.practice_volume > baseline_vector.practice_volume

def test_coverage_ratio_reflects_only_practiced_topics_before_as_of() -> None:
    dataset = ClientDataset(
        practice_attempts=(
            PracticeAttemptRecord(
                topic="dynamic_programming",
                category="dsa",
                difficulty="hard",
                confidence=7.0,
                occurred_at=AS_OF - timedelta(days=1),
            ),
        )
    )
    vector = extract_features(dataset, AS_OF, TARGET)  # TARGET wants dynamic_programming + graphs
    assert vector.coverage_ratio == pytest.approx(0.5)

def test_round_type_ordinal_matches_declared_order() -> None:
    oa = extract_features(ClientDataset(), AS_OF, TargetRound(round_type="OA", topics_tested=()))
    final = extract_features(ClientDataset(), AS_OF, TargetRound(round_type="FINAL", topics_tested=()))

    assert oa.round_type_ordinal == 0.0
    assert final.round_type_ordinal == 1.0

def test_empty_dataset_produces_a_valid_all_zero_vector() -> None:
    vector = extract_features(ClientDataset(), AS_OF, TargetRound(round_type="OA", topics_tested=()))
    assert vector.mastery_mean == 0.0
    assert vector.practice_volume == 0.0
    assert vector.application_volume == 0.0
