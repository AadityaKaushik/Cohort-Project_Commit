"""Local scikit-learn trainer (US-59, P45).

Wraps a binary `SGDClassifier` -- chosen per ADR-011's spike findings and
PROJECT_PLAN.md §22.5 ("Model: Logistic regression, 25 parameters")
specifically because its entire trainable state is two fixed-shape numpy
arrays, which is what makes FedAvg's shape-matched averaging well-defined.
Training happens warm-started from the server's current global parameters
(§22.4 step 2c: "theta_i <- fit(theta(r-1), X_i, y_i)"), entirely inside
this process, over `X`/`y` that never leave it.

Two things this module deliberately does NOT do, by design: it does not
read from a database (the caller hands it already-extracted `X`/`y`, e.g.
from `feature_extractor.extract_features`), and it does not clip or add
noise to its output (that is `privacy.py`'s job, P46, kept as a separate
step so the privacy transform is auditable on its own).
"""

from __future__ import annotations

import logging

import numpy as np
from sklearn.linear_model import SGDClassifier

from app.common.parameters import COEF_SHAPE, INTERCEPT_SHAPE
from app.common.schema import FEATURE_COUNT

logger = logging.getLogger(__name__)

CLASSES = np.array([0, 1])

# §22.4 step 2b: "if n_i < 5 examples: abstain from this round, log the reason".
MIN_LOCAL_EXAMPLES = 5


class InsufficientLocalDataError(RuntimeError):
    """Raised when a client has too few local examples to train this round.

    Per §22.4 step 2b this is an abstention, not a crash: the caller is
    expected to catch this, skip the round for this client, and let the
    server proceed (or refuse) based on whoever else participated.
    """


def new_local_model(seed: int) -> SGDClassifier:
    """A fresh local model, deterministic given a fixed seed (`random_state`
    is the only source of randomness `SGDClassifier` has under `partial_fit`:
    it controls sample-visit order, and nothing else here is stochastic)."""
    return SGDClassifier(loss="log_loss", random_state=seed, warm_start=True)


def _prime(model: SGDClassifier) -> SGDClassifier:
    """`SGDClassifier` has no `coef_`/`intercept_` until a `partial_fit` call
    declares the full class set (ADR-011 gotcha 4). Priming on synthetic,
    all-zero rows -- never real user data -- allocates shape-correct
    parameters that `set_parameters` immediately overwrites; the tiny,
    degenerate gradient step priming itself performs is irrelevant because
    of that overwrite, and using real data here would need every client's
    first local example to include both classes, which is not guaranteed.
    """
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
    """One warm-started local training step (§22.4 step 2c).

    Deterministic: the same `(global_parameters, x, y, seed)` always
    produces bit-identical resulting weights, since `SGDClassifier` with a
    fixed `random_state` has no other source of randomness under
    `partial_fit` over fixed data.
    """
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
