from __future__ import annotations

from functools import lru_cache

import numpy as np
from sklearn.linear_model import LogisticRegression

from app.demo.synthetic import check_set, generate_examples


def accuracy(parameters: list[np.ndarray], x: np.ndarray, y: np.ndarray) -> float:
    coef, intercept = parameters
    predicted = ((x @ coef.reshape(-1) + intercept[0]) > 0).astype(int)
    return float((predicted == y).mean())


def check_set_accuracy(parameters: list[np.ndarray]) -> float:
    x, y = _check_set()
    return accuracy(parameters, x, y)


@lru_cache(maxsize=1)
def central_ceiling() -> float:
    """What one model trained on all the synthetic data in one place reaches on the check set."""
    x_train, y_train = generate_examples(np.random.default_rng(5), 3000)
    x, y = _check_set()
    return float(LogisticRegression(max_iter=2000).fit(x_train, y_train).score(x, y))


@lru_cache(maxsize=1)
def _check_set() -> tuple[np.ndarray, np.ndarray]:
    return check_set()
