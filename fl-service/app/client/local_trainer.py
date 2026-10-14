from __future__ import annotations

import logging

import numpy as np
from sklearn.linear_model import SGDClassifier

from app.common.parameters import COEF_SHAPE, INTERCEPT_SHAPE
from app.common.schema import FEATURE_COUNT

logger = logging.getLogger(__name__)

CLASSES = np.array([0, 1])

MIN_LOCAL_EXAMPLES = 5

class InsufficientLocalDataError(RuntimeError):
    """Raised when a client has too few local examples to train this round.

    Per §22.4 step 2b this is an abstention, not a crash: the caller is
    expected to catch this, skip the round for this client, and let the
    server proceed (or refuse) based on whoever else participated.
    """

def new_local_model(seed: int) -> SGDClassifier:
    return SGDClassifier(loss="log_loss", random_state=seed, warm_start=True)

def _prime(model: SGDClassifier) -> SGDClassifier:
    zeros = np.zeros((2, FEATURE_COUNT))
    model.partial_fit(zeros, CLASSES, classes=CLASSES)
    return model

def get_parameters(model: SGDClassifier) -> list[np.ndarray]:
    return [model.coef_.copy(), model.intercept_.copy()]

def set_parameters(model: SGDClassifier, parameters: list[np.ndarray]) -> None:
    coef, intercept = parameters
    if coef.shape != COEF_SHAPE or intercept.shape != INTERCEPT_SHAPE:
        raise ValueError(
            f"unexpected parameter shapes: coef={coef.shape}, intercept={intercept.shape} "
            f"(expected {COEF_SHAPE}, {INTERCEPT_SHAPE})"
        )
    model.coef_ = coef.copy()
    model.intercept_ = intercept.copy()

def train_local_round(
    global_parameters: list[np.ndarray],
    x: np.ndarray,
    y: np.ndarray,
    seed: int,
) -> list[np.ndarray]:
    if x.shape[0] != y.shape[0]:
        raise ValueError(f"x has {x.shape[0]} rows but y has {y.shape[0]}")
    if x.shape[0] < MIN_LOCAL_EXAMPLES:
        logger.warning(
            "abstaining from local training round: %d example(s) < minimum %d",
            x.shape[0],
            MIN_LOCAL_EXAMPLES,
        )
        raise InsufficientLocalDataError(
            f"{x.shape[0]} local example(s) is below the minimum of {MIN_LOCAL_EXAMPLES}"
        )
    if x.shape[1] != FEATURE_COUNT:
        raise ValueError(f"x has {x.shape[1]} columns, expected {FEATURE_COUNT} (schema v1)")

    model = new_local_model(seed)
    _prime(model)
    set_parameters(model, global_parameters)
    model.partial_fit(x, y, classes=CLASSES)
    return get_parameters(model)
