from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np

from app.common.schema import FEATURE_COUNT

@dataclass(frozen=True)
class ClientFeatures:
    schema_version: int
    features: np.ndarray  # shape (24,), FEATURE_COUNT: the client's current vector
    examples_x: np.ndarray = field(default_factory=lambda: np.zeros((0, FEATURE_COUNT)))  # one row per past round
    examples_y: np.ndarray = field(default_factory=lambda: np.zeros(0, dtype=int))  # 1 = rated the round 4 or 5

    @property
    def example_count(self) -> int:
        return int(self.examples_y.shape[0])

@dataclass
class ClientFeatureStore:
    _by_client: dict[str, ClientFeatures] = field(default_factory=dict)

    def put(
        self,
        client_id: str,
        schema_version: int,
        features: np.ndarray,
        examples_x: np.ndarray | None = None,
        examples_y: np.ndarray | None = None,
    ) -> None:
        x = np.zeros((0, FEATURE_COUNT)) if examples_x is None else np.asarray(examples_x, dtype=np.float64).reshape(-1, FEATURE_COUNT)
        y = np.zeros(0, dtype=int) if examples_y is None else np.asarray(examples_y, dtype=int).reshape(-1)
        if x.shape[0] != y.shape[0]:
            raise ValueError(f"{x.shape[0]} example rows but {y.shape[0]} labels")
        self._by_client[client_id] = ClientFeatures(schema_version=schema_version, features=features, examples_x=x, examples_y=y)

    def get(self, client_id: str) -> ClientFeatures | None:
        return self._by_client.get(client_id)

    def delete(self, client_id: str) -> bool:
        return self._by_client.pop(client_id, None) is not None

    def all(self) -> dict[str, ClientFeatures]:
        return dict(self._by_client)

    def clear(self) -> None:
        self._by_client.clear()
