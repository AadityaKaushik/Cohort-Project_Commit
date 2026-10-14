from __future__ import annotations

import numpy as np
import pytest

from app.common.parameters import zero_parameters
from app.server.model_registry import ModelRegistry, run_and_publish_round
from app.server.strategy import MinCohortFedAvg

def test_registry_starts_at_version_zero() -> None:
    registry = ModelRegistry(zero_parameters())
    assert registry.current.version == 0
    assert registry.current.round_number == 0

def test_publish_appends_a_new_version_without_touching_the_old_one() -> None:
    registry = ModelRegistry(zero_parameters())
    version_zero = registry.current

    new_parameters = [np.full((1, 24), 0.5), np.array([0.2])]
    published = registry.publish(new_parameters, round_number=1, participant_count=3)

    assert published.version == 1
    assert registry.current.version == 1
    assert registry.get(0) == version_zero
    assert len(registry.history) == 2

def test_a_refused_round_does_not_publish_any_new_version() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)
    too_few_updates = [("a", np.full(25, 0.05), 5), ("b", np.full(25, 0.05), 5)]

    result = run_and_publish_round(registry, strategy, server_round=1, client_updates=too_few_updates)

    assert result.accepted is False
    assert registry.current.version == 0
    assert len(registry.history) == 1

def test_an_accepted_round_publishes_a_new_version() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)
    client_updates = [(f"c{i}", np.full(25, 0.02), 5) for i in range(3)]

    result = run_and_publish_round(registry, strategy, server_round=1, client_updates=client_updates)

    assert result.accepted is True
    assert registry.current.version == 1
    assert registry.current.round_number == 1

def test_a_failed_round_after_a_successful_one_still_serves_the_successful_version() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)

    good_updates = [(f"c{i}", np.full(25, 0.02), 5) for i in range(3)]
    round_one = run_and_publish_round(registry, strategy, server_round=1, client_updates=good_updates)
    assert round_one.accepted is True
    version_after_round_one = registry.current

    too_few_updates = [("a", np.full(25, 0.05), 5), ("b", np.full(25, 0.05), 5)]
    round_two = run_and_publish_round(registry, strategy, server_round=2, client_updates=too_few_updates)
    assert round_two.accepted is False

    assert registry.current == version_after_round_one
    assert registry.current.version == 1
    assert len(registry.history) == 2  # version 0 (seed) + version 1 (round 1) only

def test_a_fully_poisoned_round_after_a_successful_one_also_leaves_the_model_untouched() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)

    good_updates = [(f"c{i}", np.full(25, 0.02), 5) for i in range(3)]
    run_and_publish_round(registry, strategy, server_round=1, client_updates=good_updates)
    version_after_round_one = registry.current

    all_poisoned = [(f"p{i}", np.full(25, 100.0), 5) for i in range(3)]
    result = run_and_publish_round(registry, strategy, server_round=2, client_updates=all_poisoned)

    assert result.accepted is False
    assert registry.current == version_after_round_one

def test_get_an_unknown_version_raises_key_error() -> None:
    registry = ModelRegistry(zero_parameters())
    with pytest.raises(KeyError):
        registry.get(99)
