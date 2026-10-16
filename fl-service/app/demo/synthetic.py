from __future__ import annotations

import numpy as np

from app.common.schema import FEATURE_BOUNDS, FEATURE_NAMES, FeatureVector

SYNTHETIC_PREFIX = "synthetic-client-"

_SHARE_FEATURES = ("share_dsa", "share_technical", "share_behavioural", "share_system_design")

# Same step sizes the Java and Python extractors produce for these count-based features.
_DISCRETE_STEPS = {
    "practice_consistency": 8,
    "resume_variants": 5,
    "round_type_ordinal": 6,
}

def synthetic_client_id(index: int) -> str:
    if index < 1:
        raise ValueError(f"synthetic client index starts at 1, got {index}")
    return f"{SYNTHETIC_PREFIX}{index:02d}"

def is_synthetic(client_id: str) -> bool:
    return client_id.startswith(SYNTHETIC_PREFIX)

def generate_feature_vector(rng: np.random.Generator) -> FeatureVector:
    values: dict[str, float] = {}
    for name in FEATURE_NAMES:
        lo, hi = FEATURE_BOUNDS[name]
        if name in _DISCRETE_STEPS:
            steps = _DISCRETE_STEPS[name]
            values[name] = float(rng.integers(0, steps + 1)) / steps
        else:
            values[name] = float(rng.uniform(lo, hi))

    values["mastery_min"] = values["mastery_mean"] * float(rng.uniform(0.3, 1.0))
    values["mastery_std"] = min(values["mastery_std"], values["mastery_mean"] - values["mastery_min"])

    shares = rng.dirichlet(np.ones(len(_SHARE_FEATURES)))
    for name, share in zip(_SHARE_FEATURES, shares):
        values[name] = float(share)

    return FeatureVector(**values)

def generate_synthetic_clients(count: int, seed: int) -> dict[str, FeatureVector]:
    if count < 0:
        raise ValueError(f"count must not be negative, got {count}")
    rng = np.random.default_rng(seed)
    return {synthetic_client_id(i): generate_feature_vector(rng) for i in range(1, count + 1)}
