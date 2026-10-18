from __future__ import annotations

import logging

import numpy as np
import pytest

from app.common.parameters import flatten, zero_parameters
from app.server.round_runner import run_round
from app.server.strategy import (
    DEFAULT_CLIP_NORM,
    DEFAULT_POISONING_NORM_MULTIPLIER,
    MinCohortFedAvg,
)

def test_an_over_norm_update_is_filtered_out_of_aggregation() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    honest_deltas = [np.full(25, 0.1), np.full(25, 0.2)]
    poisoned_delta = np.full(25, 100.0)  # norm far above S * tau
    client_updates = [
        ("honest-a", honest_deltas[0], 5),
        ("honest-b", honest_deltas[1], 5),
        ("attacker", poisoned_delta, 5),
    ]

    result = run_round(strategy, 1, zero_parameters(), client_updates)

    assert result.accepted is True
    assert result.filtered_count == 1
    # The aggregated result must reflect only the two honest updates.
    expected_flat = np.mean(honest_deltas, axis=0)
    assert flatten(result.parameters) == pytest.approx(expected_flat)

def test_filtering_a_poisoned_update_is_logged(caplog: pytest.LogCaptureFixture) -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    client_updates = [
        ("honest-a", np.full(25, 0.1), 5),
        ("honest-b", np.full(25, 0.2), 5),
        ("attacker", np.full(25, 100.0), 5),
    ]

    with caplog.at_level(logging.WARNING, logger="app.server.strategy"):
        run_round(strategy, 1, zero_parameters(), client_updates)

    assert any("dropping client attacker" in record.message for record in caplog.records)

def test_an_update_exactly_at_the_threshold_is_kept() -> None:
    strategy = MinCohortFedAvg(
        min_cohort=3, clip_norm=DEFAULT_CLIP_NORM, poisoning_norm_multiplier=DEFAULT_POISONING_NORM_MULTIPLIER
    )
    threshold = strategy.poisoning_threshold
    at_threshold = np.zeros(25)
    at_threshold[0] = threshold  # L2 norm exactly == threshold

    client_updates = [
        ("a", np.full(25, 0.1), 5),
        ("b", np.full(25, 0.1), 5),
        ("borderline", at_threshold, 5),
    ]
    result = run_round(strategy, 1, zero_parameters(), client_updates)

    assert result.accepted is True
    assert result.filtered_count == 0

def test_an_update_just_above_the_threshold_is_filtered() -> None:
    strategy = MinCohortFedAvg(
        min_cohort=3, clip_norm=DEFAULT_CLIP_NORM, poisoning_norm_multiplier=DEFAULT_POISONING_NORM_MULTIPLIER
    )
    threshold = strategy.poisoning_threshold
    just_over = np.zeros(25)
    just_over[0] = threshold * 1.01

    client_updates = [
        ("a", np.full(25, 0.1), 5),
        ("b", np.full(25, 0.1), 5),
        ("over", just_over, 5),
    ]
    result = run_round(strategy, 1, zero_parameters(), client_updates)

    assert result.accepted is True
    assert result.filtered_count == 1

def test_round_is_refused_if_filtering_removes_every_participant() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    poisoned = np.full(25, 100.0)
    client_updates = [("a", poisoned, 5), ("b", poisoned, 5), ("c", poisoned, 5)]

    result = run_round(strategy, 1, zero_parameters(), client_updates)

    assert result.accepted is False
    assert result.reason is not None and "filtered" in result.reason.lower()

def test_min_cohort_check_runs_before_poisoning_filter_uses_raw_count() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    client_updates = [("a", np.full(25, 0.1), 5), ("b", np.full(25, 0.1), 5)]

    result = run_round(strategy, 1, zero_parameters(), client_updates)

    assert result.accepted is False
    assert result.participant_count == 2
    assert "refused" in (result.reason or "").lower()
    assert "below the minimum cohort" in (result.reason or "")


def test_an_honest_clipped_and_noised_update_is_kept() -> None:
    from app.client.privacy import privatize_update

    strategy = MinCohortFedAvg(min_cohort=3)
    rng = np.random.default_rng(42)
    updates = [(f"honest-{i}", privatize_update(np.full(25, 5.0), rng), 5) for i in range(200)]

    result = run_round(strategy, 1, zero_parameters(), updates)

    assert result.accepted is True
    assert result.filtered_count == 0


def test_threshold_allows_for_the_noise_but_not_for_an_unclipped_update() -> None:
    strategy = MinCohortFedAvg()
    assert strategy.poisoning_threshold > 5.1
    assert np.linalg.norm(np.full(25, 3.0)) > strategy.poisoning_threshold
