"""A real `flwr.client.NumPyClient` wrapping an `SGDClassifier` local model.

This is the client-side shape Sprint 4's `app/client/local_trainer.py` (P45)
will refine: a linear model whose entire parameter set is exactly two numpy
arrays (`coef_`, `intercept_`), which is what makes it FedAvg-friendly --
`flwr.server.strategy.aggregate.aggregate` averages same-shaped ndarray
lists, so the model family has to be chosen with that in mind (a decision
tree or anything with a non-fixed-shape parameter set would NOT work here).
"""

from __future__ import annotations

import numpy as np
from flwr.client import NumPyClient
from sklearn.linear_model import SGDClassifier

from spike.data import ClientShard

CLASSES = np.array([0, 1])


def new_local_model() -> SGDClassifier:
    """A fresh local model, deterministic given a fixed seed."""
    return SGDClassifier(loss="log_loss", random_state=0, warm_start=True)


class SklearnFedAvgClient(NumPyClient):
    """Wraps one client's local shard + model behind Flower's NumPyClient contract."""

    def __init__(self, shard: ClientShard) -> None:
        self.shard = shard
        self.model = new_local_model()
        # SGDClassifier has no coef_/intercept_ until the first partial_fit call
        # that declares the class set -- so every client "primes" its own model
        # once at construction, independent of whatever global parameters it
        # later receives (get_parameters/fit still overwrite these afterwards).
        self.model.partial_fit(self.shard.x_train[:1], self.shard.y_train[:1], classes=CLASSES)

    def get_parameters(self, config: dict) -> list[np.ndarray]:  # noqa: ARG002 -- Flower's fixed signature
        return [self.model.coef_.copy(), self.model.intercept_.copy()]

    def set_parameters(self, parameters: list[np.ndarray]) -> None:
        self.model.coef_ = parameters[0].copy()
        self.model.intercept_ = parameters[1].copy()

    def fit(self, parameters: list[np.ndarray], config: dict) -> tuple[list[np.ndarray], int, dict]:
        self.set_parameters(parameters)
        # One local epoch over this client's own shard only -- the whole point
        # of FL is that `shard.x_train`/`shard.y_train` never leave this object.
        self.model.partial_fit(self.shard.x_train, self.shard.y_train, classes=CLASSES)
        return self.get_parameters(config), len(self.shard.x_train), {}

    def evaluate(self, parameters: list[np.ndarray], config: dict) -> tuple[float, int, dict]:
        self.set_parameters(parameters)
        accuracy = float(self.model.score(self.shard.x_train, self.shard.y_train))
        return 1.0 - accuracy, len(self.shard.x_train), {"accuracy": accuracy}
