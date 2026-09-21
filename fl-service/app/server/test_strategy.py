"""P47's mandatory (and critical) verify (BUILD_PHASES_JAVA.md): a 2-client
round is refused, and the refusal is logged, when the minimum cohort size
is higher than 2. This protects against small-cohort re-identification
risk (§22.4 step 3a) and must never be relaxed to make a test pass."""

from __future__ import annotations

import logging

import numpy as np
import pytest

from app.common.parameters import flatten, zero_parameters
from app.server.round_runner import run_round
from app.server.strategy import MinCohortFedAvg


def test_two_client_round_is_refused_when_min_cohort_is_three(caplog: pytest.LogCaptureFixture) -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    previous = zero_parameters()
    client_updates = [
        ("client-a", np.full(25, 0.1), 10),
        ("client-b", np.full(25, 0.2), 10),
    ]

    with caplog.at_level(logging.WARNING, logger="app.server.strategy"):
        result = run_round(strategy, server_round=1, previous_parameters=previous, client_updates=client_updates)

    assert result.accepted is False
    assert result.participant_count == 2
    assert result.reason is not None and "refused" in result.reason.lower()
    assert any("refused" in record.message.lower() for record in caplog.records)


def test_refused_round_leaves_the_previous_parameters_unchanged() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    previous = [np.full((1, 24), 0.7), np.array([0.4])]
    result = run_round(
        strategy,
        server_round=1,
        previous_parameters=previous,
        client_updates=[("a", np.ones(25), 5), ("b", np.ones(25), 5)],
    )

    assert result.accepted is False
    assert all(np.array_equal(p, prev_p) for p, prev_p in zip(result.parameters, previous))


def test_one_client_round_is_refused_too() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    result = run_round(strategy, 1, zero_parameters(), [("solo", np.ones(25), 3)])
    assert result.accepted is False
    assert result.participant_count == 1


def test_round_with_exactly_min_cohort_participants_is_accepted() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    previous = zero_parameters()
    deltas = [np.full(25, 0.3), np.full(25, 0.6), np.full(25, 0.9)]
    client_updates = [(f"client-{i}", d, 10) for i, d in enumerate(deltas)]

    result = run_round(strategy, 1, previous, client_updates)

    assert result.accepted is True
    assert result.participant_count == 3
    expected_flat = np.mean(deltas, axis=0)  # previous is zero, so theta(r) = mean(delta)
    assert flatten(result.parameters) == pytest.approx(expected_flat)


def test_four_clients_above_min_cohort_is_accepted() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    client_updates = [(f"c{i}", np.full(25, 0.1 * i), 5) for i in range(1, 5)]
    result = run_round(strategy, 1, zero_parameters(), client_updates)
    assert result.accepted is True
    assert result.participant_count == 4


def test_aggregation_is_uniform_weighted_ignoring_num_examples() -> None:
    """Deliberately NOT weighted by client sample count (§22.4's rationale:
    that would leak each client's job-search volume). Wildly different
    num_examples must not change the aggregated result."""
    strategy = MinCohortFedAvg(min_cohort=3)
    deltas = [np.full(25, 0.1), np.full(25, 0.2), np.full(25, 0.3)]
    client_updates = [("a", deltas[0], 1), ("b", deltas[1], 100_000), ("c", deltas[2], 1)]

    result = run_round(strategy, 1, zero_parameters(), client_updates)

    expected_flat = np.mean(deltas, axis=0)
    assert flatten(result.parameters) == pytest.approx(expected_flat)


def test_aggregation_adds_the_mean_delta_to_the_previous_parameters() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    previous = [np.full((1, 24), 0.5), np.array([0.1])]
    deltas = [np.full(25, 0.2), np.full(25, 0.4), np.full(25, 0.6)]
    client_updates = [(f"c{i}", d, 5) for i, d in enumerate(deltas)]

    result = run_round(strategy, 1, previous, client_updates)

    expected_flat = flatten(previous) + np.mean(deltas, axis=0)
    assert flatten(result.parameters) == pytest.approx(expected_flat)


def test_aggregate_fit_requires_previous_parameters_to_be_set_first() -> None:
    strategy = MinCohortFedAvg(min_cohort=3)
    with pytest.raises(RuntimeError):
        strategy.aggregate_fit(1, [], [])
