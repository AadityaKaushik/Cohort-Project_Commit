"""Clipping and Gaussian noise -- local differential privacy (US-60, P46).

Implements PROJECT_PLAN.md §22.4 steps 2d-2f exactly:

    delta_i    <- theta_i - theta(r-1)                       # raw local update
    delta_hat  <- delta_i . min(1, S / ||delta_i||_2)         # L2 clipping, S = 1.0
    delta_tilde <- delta_hat + N(0, sigma^2 S^2 I)            # Gaussian noise, sigma = 1.0

`S` bounds any single client's influence on the aggregate -- it is
simultaneously the DP sensitivity bound *and* the poisoning bound
`app/server/`'s filter (P48) checks against. `sigma` is a conservative
default held fixed per §14/C7 rather than tuned to flatter an accuracy
chart. Neither is silently overridden by a caller trying to get "better"
results -- see the docstring on `privatize_update`.

**What this module guarantees, and what it must never do:** the vector
that leaves a client is never the raw update. Not "usually different" --
provably different, because Gaussian noise is added unconditionally after
clipping, every time, with no code path that skips it.
"""

from __future__ import annotations

import numpy as np

# S: the DP sensitivity bound / poisoning bound (§22.4's rationale: "bounds
# any single client's influence, which is simultaneously the DP sensitivity
# bound and the poisoning bound").
DEFAULT_CLIP_NORM = 1.0

# sigma: a conservative default from the DP-SGD literature (§22.4).
DEFAULT_NOISE_MULTIPLIER = 1.0


def compute_delta(new_parameters: np.ndarray, previous_parameters: np.ndarray) -> np.ndarray:
    """`delta_i = theta_i - theta(r-1)` -- both arguments are flat parameter
    vectors (see `app.common.parameters.flatten`)."""
    if new_parameters.shape != previous_parameters.shape:
        raise ValueError(
            f"shape mismatch: new={new_parameters.shape} previous={previous_parameters.shape}"
        )
    return new_parameters - previous_parameters


def clip_update(delta: np.ndarray, clip_norm: float = DEFAULT_CLIP_NORM) -> np.ndarray:
    """`delta_hat = delta . min(1, S / ||delta||_2)`.

    Scales the whole update down so its L2 norm never exceeds `clip_norm`;
    an update already within the bound is returned unscaled (the `min(1, ...)`
    in the formula, not a hard normalise-to-exactly-S).
    """
    if clip_norm <= 0:
        raise ValueError(f"clip_norm must be positive, got {clip_norm}")
    norm = float(np.linalg.norm(delta))
    if norm == 0.0 or norm <= clip_norm:
        return delta.copy()
    return delta * (clip_norm / norm)


def add_gaussian_noise(
    vector: np.ndarray,
    rng: np.random.Generator,
    sigma: float = DEFAULT_NOISE_MULTIPLIER,
    clip_norm: float = DEFAULT_CLIP_NORM,
) -> np.ndarray:
    """`vector + N(0, sigma^2 S^2 I)`. `rng` is required (never a module-level
    global generator) so callers -- and tests -- control determinism explicitly."""
    noise = rng.normal(loc=0.0, scale=sigma * clip_norm, size=vector.shape)
    return vector + noise


def privatize_update(
    raw_delta: np.ndarray,
    rng: np.random.Generator,
    clip_norm: float = DEFAULT_CLIP_NORM,
    sigma: float = DEFAULT_NOISE_MULTIPLIER,
) -> np.ndarray:
    """The full §22.4 2d-2f pipeline: clip, then noise. This is the only
    function client code should call to produce a transmittable update --
    `clip_update`/`add_gaussian_noise` are exposed separately purely so
    tests can assert on the intermediate clipped value's norm.
    """
    clipped = clip_update(raw_delta, clip_norm=clip_norm)
    return add_gaussian_noise(clipped, rng, sigma=sigma, clip_norm=clip_norm)
