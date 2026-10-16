from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np

@dataclass(frozen=True)
class ClientFeatures:
    schema_version: int
    features: np.ndarray  # shape (24,), FEATURE_COUNT

@dataclass
class ClientFeatureStore:
    _by_client: dict[str, ClientFeatures] = field(default_factory=dict)

    def put(self, client_id: str, schema_version: int, features: np.ndarray) -> None:
        self._by_client[client_id] = ClientFeatures(schema_version=schema_version, features=features)

    def get(self, client_id: str) -> ClientFeatures | None:
        return self._by_client.get(client_id)

    def delete(self, client_id: str) -> bool:
        return self._by_client.pop(client_id, None) is not None

    def all(self) -> dict[str, ClientFeatures]:
        return dict(self._by_client)

    def clear(self) -> None:
        self._by_client.clear()
