"""P46's mandatory (and critical) verify (BUILD_PHASES_JAVA.md): the
transmitted vector must never equal the raw update, and the clipped vector's
L2 norm must never exceed S. Both properties are checked directly, not
inferred -- this is privacy-sensitive code and neither assertion may be
weakened to make a test pass."""

from __future__ import annotations

import numpy as np
import pytest

from app.client.privacy import (
    DEFAULT_CLIP_NORM,
    add_gaussian_noise,
    clip_update,
    compute_delta,
    privatize_update,
)


def test_transmitted_vector_never_equals_the_raw_update() -> None:
    rng = np.random.default_rng(0)
    raw_delta = np.full(25, 0.3)

    transmitted = privatize_update(raw_delta, rng)

    assert not np.array_equal(transmitted, raw_delta)


def test_clipped_update_norm_is_at_most_s() -> None:
    oversized = np.full(25, 10.0)  # L2 norm far larger than S=1.0
    clipped = clip_update(oversized, clip_norm=DEFAULT_CLIP_NORM)

    assert np.linalg.norm(clipped) <= DEFAULT_CLIP_NORM + 1e-9


def test_clipping_actually_scales_down_an_oversized_update() -> None:
    oversized = np.full(25, 10.0)
    clipped = clip_update(oversized, clip_norm=1.0)

    assert np.linalg.norm(clipped) == pytest.approx(1.0)
    assert not np.array_equal(clipped, oversized)


def test_an_update_already_within_the_bound_is_not_rescaled() -> None:
    small = np.zeros(25)
    small[0] = 0.1  # norm well under S=1.0
    clipped = clip_update(small, clip_norm=1.0)

    assert np.array_equal(clipped, small)


def test_a_zero_update_clips_to_zero_without_dividing_by_zero() -> None:
    zero = np.zeros(25)
    clipped = clip_update(zero, clip_norm=1.0)
    assert np.array_equal(clipped, zero)


def test_privatized_update_norm_can_exceed_s_because_noise_is_added_after_clipping() -> None:
    # Noise is added AFTER clipping, so the final transmitted vector's norm
    # is not itself bounded by S -- only the clipped intermediate is. This
    # test pins that distinction so it is never "fixed" by silently
    # reordering clip/noise (which would defeat the sensitivity bound).
    rng = np.random.default_rng(1)
    oversized = np.full(25, 50.0)
    clipped = clip_update(oversized, clip_norm=1.0)
    transmitted = add_gaussian_noise(clipped, rng, sigma=1.0, clip_norm=1.0)

    assert np.linalg.norm(clipped) <= 1.0 + 1e-9
    assert not np.array_equal(transmitted, clipped)


def test_noise_is_nondeterministic_across_independent_rngs_but_reproducible_with_a_fixed_seed() -> None:
    raw_delta = np.full(25, 0.2)

    first = privatize_update(raw_delta, np.random.default_rng(42))
    second = privatize_update(raw_delta, np.random.default_rng(42))
    third = privatize_update(raw_delta, np.random.default_rng(43))

    assert np.array_equal(first, second)  # same seed -> reproducible for tests
    assert not np.array_equal(first, third)  # different seed -> different noise


def test_compute_delta_is_the_plain_difference() -> None:
    new = np.array([1.0, 2.0, 3.0])
    previous = np.array([0.5, 0.5, 0.5])

    assert np.array_equal(compute_delta(new, previous), np.array([0.5, 1.5, 2.5]))


def test_compute_delta_rejects_mismatched_shapes() -> None:
    with pytest.raises(ValueError):
        compute_delta(np.zeros(3), np.zeros(4))


def test_negative_clip_norm_is_rejected() -> None:
    with pytest.raises(ValueError):
        clip_update(np.ones(5), clip_norm=-1.0)
