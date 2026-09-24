"""P55 (US-63): the Privacy Ledger's core honesty property -- the vector
recorded for a client is the REAL output of `app.client.privacy`'s
clip-then-noise pipeline, not a placeholder, and it is provably different
from what that client actually exported."""

from __future__ import annotations

import numpy as np

from app.client.privacy import DEFAULT_CLIP_NORM, add_gaussian_noise, clip_update
from app.common.schema import FEATURE_COUNT
from app.server.round_orchestrator import run_privacy_round
from app.server.strategy import DEFAULT_MIN_COHORT


def _features(seed: int, scale: float = 1.0) -> np.ndarray:
    return np.random.default_rng(seed).normal(size=FEATURE_COUNT) * scale


def test_transmitted_vector_differs_from_the_raw_exported_vector() -> None:
    raw = _features(1)
    features_by_client = {"user-1": (1, raw)}

    outcome = run_privacy_round(features_by_client, round_number=1, rng=np.random.default_rng(42))

    entry = outcome.entries[0]
    assert entry.transmitted_vector != raw.tolist()
    assert not np.allclose(entry.transmitted_vector, raw)


def test_transmitted_vector_is_exactly_what_privacy_py_would_produce_for_the_same_inputs() -> None:
    """Not a re-derivation of the formula -- literally calls the same
    `app.client.privacy` functions with the same seeded rng and asserts
    byte-identical output, proving round_orchestrator does not run a
    second, potentially-drifted privacy implementation."""
    raw = _features(7, scale=2.0)
    rng_for_orchestrator = np.random.default_rng(99)
    rng_for_direct_call = np.random.default_rng(99)

    outcome = run_privacy_round({"user-1": (1, raw)}, round_number=1, rng=rng_for_orchestrator)

    expected_clipped = clip_update(raw, clip_norm=DEFAULT_CLIP_NORM)
    expected_noised = add_gaussian_noise(expected_clipped, rng_for_direct_call, clip_norm=DEFAULT_CLIP_NORM)

    entry = outcome.entries[0]
    assert entry.transmitted_vector == expected_noised.tolist()
    assert entry.post_clip_norm == float(np.linalg.norm(expected_clipped))
    assert entry.pre_clip_norm == float(np.linalg.norm(raw))


def test_post_clip_norm_never_exceeds_the_clip_bound() -> None:
    huge = np.full(FEATURE_COUNT, 1000.0)  # deliberately far over the clip norm

    outcome = run_privacy_round({"user-1": (1, huge)}, round_number=1, rng=np.random.default_rng(1))

    assert outcome.entries[0].post_clip_norm <= DEFAULT_CLIP_NORM + 1e-9


def test_round_below_min_cohort_is_refused_but_entries_are_still_recorded() -> None:
    """US-63: 'a log of everything my client has ever sent' -- a client's
    contribution is recorded even when the round as a whole is refused."""
    features_by_client = {f"user-{i}": (1, _features(i)) for i in range(DEFAULT_MIN_COHORT - 1)}

    outcome = run_privacy_round(features_by_client, round_number=5, rng=np.random.default_rng(1))

    assert outcome.accepted is False
    assert outcome.reason is not None
    assert outcome.cohort_size == DEFAULT_MIN_COHORT - 1
    assert len(outcome.entries) == DEFAULT_MIN_COHORT - 1


def test_round_at_or_above_min_cohort_is_accepted() -> None:
    features_by_client = {f"user-{i}": (1, _features(i)) for i in range(DEFAULT_MIN_COHORT)}

    outcome = run_privacy_round(features_by_client, round_number=6, rng=np.random.default_rng(1))

    assert outcome.accepted is True
    assert outcome.reason is None


def test_every_entry_has_a_positive_epsilon() -> None:
    outcome = run_privacy_round({"user-1": (1, _features(3))}, round_number=1, rng=np.random.default_rng(1))
    assert outcome.entries[0].epsilon > 0
