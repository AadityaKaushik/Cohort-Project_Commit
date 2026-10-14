from __future__ import annotations

import numpy as np

from app.common.schema import FEATURE_COUNT

COEF_SHAPE = (1, FEATURE_COUNT)
INTERCEPT_SHAPE = (1,)
PARAMETER_COUNT = FEATURE_COUNT + 1  # 24 coefficients + 1 intercept = 25

def flatten(parameters: list[np.ndarray]) -> np.ndarray:
    coef, intercept = parameters
    if coef.shape != COEF_SHAPE or intercept.shape != INTERCEPT_SHAPE:
        raise ValueError(
            f"unexpected parameter shapes: coef={coef.shape}, intercept={intercept.shape} "
            f"(expected {COEF_SHAPE}, {INTERCEPT_SHAPE})"
        )
    return np.concatenate([coef.reshape(-1), intercept.reshape(-1)]).astype(np.float64)

def unflatten(flat: np.ndarray) -> list[np.ndarray]:
    if flat.shape != (PARAMETER_COUNT,):
        raise ValueError(f"expected a flat vector of shape ({PARAMETER_COUNT},), got {flat.shape}")
    coef = flat[:FEATURE_COUNT].reshape(COEF_SHAPE).copy()
    intercept = flat[FEATURE_COUNT:].reshape(INTERCEPT_SHAPE).copy()
    return [coef, intercept]

def zero_parameters() -> list[np.ndarray]:
    return [np.zeros(COEF_SHAPE), np.zeros(INTERCEPT_SHAPE)]
