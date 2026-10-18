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

# The hidden rule synthetic labels follow. Real users are not assumed to follow it; it only gives the
# demo something learnable, so a check set can show the shared model getting better round by round.
_TRUE_WEIGHTS = {
    "coverage_ratio": 3.0,
    "mastery_mean": 2.5,
    "practice_consistency": 2.0,
    "recent_self_rating": 2.0,
    "prep_lead_time": 1.5,
    "interview_experience": 1.0,
    "hard_ratio": -1.5,
}
_TRUE_WEIGHT_VECTOR = np.array([_TRUE_WEIGHTS.get(name, 0.0) for name in FEATURE_NAMES])
_TRUE_INTERCEPT = 0.6 - 0.5 * sum(w for w in _TRUE_WEIGHTS.values() if w > 0)

MIN_SYNTHETIC_EXAMPLES = 6
MAX_SYNTHETIC_EXAMPLES = 15
CHECK_SET_SIZE = 2000
CHECK_SET_SEED = 99


def as_array(vector: FeatureVector) -> np.ndarray:
    return np.array([float(getattr(vector, name)) for name in FEATURE_NAMES])


def label_examples(x: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    probability = 1.0 / (1.0 + np.exp(-(x @ _TRUE_WEIGHT_VECTOR + _TRUE_INTERCEPT)))
    return (rng.uniform(size=x.shape[0]) < probability).astype(int)


def generate_examples(rng: np.random.Generator, count: int) -> tuple[np.ndarray, np.ndarray]:
    x = np.array([as_array(generate_feature_vector(rng)) for _ in range(count)]).reshape(-1, len(FEATURE_NAMES))
    return x, label_examples(x, rng)


def generate_synthetic_training_clients(count: int, seed: int) -> dict[str, tuple[FeatureVector, np.ndarray, np.ndarray]]:
    """Each synthetic client gets a current vector plus 6 to 15 labelled past rounds."""
    if count < 0:
        raise ValueError(f"count must not be negative, got {count}")
    rng = np.random.default_rng(seed)
    clients: dict[str, tuple[FeatureVector, np.ndarray, np.ndarray]] = {}
    for i in range(1, count + 1):
        current = generate_feature_vector(rng)
        x, y = generate_examples(rng, int(rng.integers(MIN_SYNTHETIC_EXAMPLES, MAX_SYNTHETIC_EXAMPLES + 1)))
        clients[synthetic_client_id(i)] = (current, x, y)
    return clients


def check_set() -> tuple[np.ndarray, np.ndarray]:
    """A fixed held-out set drawn from the same hidden rule, used only to score demo models."""
    return generate_examples(np.random.default_rng(CHECK_SET_SEED), CHECK_SET_SIZE)
