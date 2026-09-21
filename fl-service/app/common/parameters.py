"""Shared parameter-shape utilities for the FL model family (US-59/60/61).

Per ADR-011 finding 2 ("A linear scikit-learn model is a clean fit for
FedAvg... its entire trainable state is exactly two fixed-shape numpy
arrays") and PROJECT_PLAN.md §22.5 ("Model: Logistic regression, 25
parameters"), Cohort's FL model is a binary `SGDClassifier` over the 24
schema features: `coef_` has shape `(1, 24)`, `intercept_` has shape `(1,)`,
so the model's entire state is exactly 25 floats -- which is also why
§22.4's transmitted delta is described as "25 floats". Flattening to one
`(25,)` vector is what makes clipping (`app/client/privacy.py`, an L2 norm
over the *whole* update) and uniform FedAvg averaging (`app/server/`)
well-defined operations, rather than something ad hoc per-array.

Both the client (P45/P46) and the server (P47/P48) import this module
rather than each re-deriving the (1, 24)/(1,) shapes, so the parameter
layout has exactly one place to change.
"""

from __future__ import annotations

import numpy as np

from app.common.schema import FEATURE_COUNT

COEF_SHAPE = (1, FEATURE_COUNT)
INTERCEPT_SHAPE = (1,)
PARAMETER_COUNT = FEATURE_COUNT + 1  # 24 coefficients + 1 intercept = 25


def flatten(parameters: list[np.ndarray]) -> np.ndarray:
    """`[coef_, intercept_]` (shapes `(1,24)`, `(1,)`) -> one `(25,)` vector."""
    coef, intercept = parameters
    if coef.shape != COEF_SHAPE or intercept.shape != INTERCEPT_SHAPE:
        raise ValueError(
            f"unexpected parameter shapes: coef={coef.shape}, intercept={intercept.shape} "
            f"(expected {COEF_SHAPE}, {INTERCEPT_SHAPE})"
        )
    return np.concatenate([coef.reshape(-1), intercept.reshape(-1)]).astype(np.float64)


def unflatten(flat: np.ndarray) -> list[np.ndarray]:
    """The inverse of `flatten`: one `(25,)` vector -> `[coef_, intercept_]`."""
    if flat.shape != (PARAMETER_COUNT,):
        raise ValueError(f"expected a flat vector of shape ({PARAMETER_COUNT},), got {flat.shape}")
    coef = flat[:FEATURE_COUNT].reshape(COEF_SHAPE).copy()
    intercept = flat[FEATURE_COUNT:].reshape(INTERCEPT_SHAPE).copy()
    return [coef, intercept]


def zero_parameters() -> list[np.ndarray]:
    """The initial global model: an all-zero linear model, shape-correct."""
    return [np.zeros(COEF_SHAPE), np.zeros(INTERCEPT_SHAPE)]
