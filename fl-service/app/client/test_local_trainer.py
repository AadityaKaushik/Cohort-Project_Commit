"""P45's mandatory verify (BUILD_PHASES_JAVA.md): local training is
deterministic under a fixed random seed -- training twice with the same
seed and data must produce bit-identical resulting weights. This matters
because a non-deterministic trainer would make P47's min-cohort and
poisoning-filter tests flaky, and violates this project's golden-file
testing philosophy (NFR-51, per ADR-011 finding 6)."""

from __future__ import annotations

import numpy as np
import pytest

from app.client.local_trainer import (
    MIN_LOCAL_EXAMPLES,
    InsufficientLocalDataError,
    get_parameters,
    new_local_model,
    train_local_round,
)
from app.common.parameters import zero_parameters
from app.common.schema import FEATURE_COUNT

SEED = 7


def _synthetic_examples(n: int, seed: int = 1) -> tuple[np.ndarray, np.ndarray]:
    rng = np.random.default_rng(seed)
    x = rng.random((n, FEATURE_COUNT))
    y = (x[:, 0] > 0.5).astype(int)
    # Guarantee both classes are present so partial_fit's class set matches.
    y[0], y[1] = 0, 1
    return x, y


def test_training_is_deterministic_under_a_fixed_seed() -> None:
    x, y = _synthetic_examples(30)
    global_parameters = zero_parameters()

    first = train_local_round(global_parameters, x, y, seed=SEED)
    second = train_local_round(global_parameters, x, y, seed=SEED)

    assert np.array_equal(first[0], second[0])  # coef_
    assert np.array_equal(first[1], second[1])  # intercept_


def test_different_seeds_can_produce_different_weights() -> None:
    x, y = _synthetic_examples(30)
    global_parameters = zero_parameters()

    first = train_local_round(global_parameters, x, y, seed=1)
    second = train_local_round(global_parameters, x, y, seed=2)

    assert not np.array_equal(first[0], second[0])


def test_trained_parameters_have_the_expected_shape() -> None:
    x, y = _synthetic_examples(10)
    result = train_local_round(zero_parameters(), x, y, seed=SEED)

    assert result[0].shape == (1, FEATURE_COUNT)
    assert result[1].shape == (1,)


def test_new_local_model_has_no_parameters_before_priming() -> None:
    model = new_local_model(SEED)
    assert not hasattr(model, "coef_")


def test_below_minimum_examples_raises_and_does_not_train() -> None:
    x, y = _synthetic_examples(MIN_LOCAL_EXAMPLES - 1)

    with pytest.raises(InsufficientLocalDataError):
        train_local_round(zero_parameters(), x, y, seed=SEED)


def test_exactly_minimum_examples_is_accepted() -> None:
    x, y = _synthetic_examples(MIN_LOCAL_EXAMPLES)
    result = train_local_round(zero_parameters(), x, y, seed=SEED)
    assert get_parameters and result[0].shape == (1, FEATURE_COUNT)


def test_warm_start_from_nonzero_global_parameters_changes_the_result() -> None:
    x, y = _synthetic_examples(30)

    from_zero = train_local_round(zero_parameters(), x, y, seed=SEED)

    nonzero_global = [np.full((1, FEATURE_COUNT), 0.5), np.array([0.2])]
    from_nonzero = train_local_round(nonzero_global, x, y, seed=SEED)

    assert not np.array_equal(from_zero[0], from_nonzero[0])
