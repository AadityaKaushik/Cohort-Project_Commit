from __future__ import annotations

import numpy as np

DEFAULT_CLIP_NORM = 1.0

DEFAULT_NOISE_MULTIPLIER = 1.0

def compute_delta(new_parameters: np.ndarray, previous_parameters: np.ndarray) -> np.ndarray:
    if new_parameters.shape != previous_parameters.shape:
        raise ValueError(
            f"shape mismatch: new={new_parameters.shape} previous={previous_parameters.shape}"
        )
    return new_parameters - previous_parameters

def clip_update(delta: np.ndarray, clip_norm: float = DEFAULT_CLIP_NORM) -> np.ndarray:
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
    """`vector + N(0, sigma^2 S^2 I)`."""
    noise = rng.normal(loc=0.0, scale=sigma * clip_norm, size=vector.shape)
    return vector + noise

def privatize_update(
    raw_delta: np.ndarray,
    rng: np.random.Generator,
    clip_norm: float = DEFAULT_CLIP_NORM,
    sigma: float = DEFAULT_NOISE_MULTIPLIER,
) -> np.ndarray:
    clipped = clip_update(raw_delta, clip_norm=clip_norm)
    return add_gaussian_noise(clipped, rng, sigma=sigma, clip_norm=clip_norm)
