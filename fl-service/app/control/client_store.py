"""In-memory store for exported client feature vectors (US-58, P51).

Java's sanitised export job (`api/.../service/fl/FlFeatureExportJob.java`)
PUTs one already-sanitised, allow-listed 24-float vector per opted-in user
here (`PUT /fl/client-features/{user_id}`, `app/control/api.py`); the
round endpoint (P55) reads from this store rather than requiring every
`/fl/round` call to re-carry every client's vector.

Deliberately in-memory, not Postgres-backed -- consistent with
`ModelRegistry` (P49) and the V19 migration's module docstring on why
fl-service holds no direct database credentials in this implementation.
Process-lifetime only: a restarted fl-service starts with an empty store,
same as `ModelRegistry` resets to model version 0.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np


@dataclass(frozen=True)
class ClientFeatures:
    schema_version: int
    features: np.ndarray  # shape (24,) -- FEATURE_COUNT, enforced by the caller


@dataclass
class ClientFeatureStore:
    """One process-lifetime dict, `client_id -> ClientFeatures`. `put` always
    overwrites -- the latest export supersedes any prior one for that
    client, matching "this is the client's *current* state," not a log."""

    _by_client: dict[str, ClientFeatures] = field(default_factory=dict)

    def put(self, client_id: str, schema_version: int, features: np.ndarray) -> None:
        self._by_client[client_id] = ClientFeatures(schema_version=schema_version, features=features)

    def get(self, client_id: str) -> ClientFeatures | None:
        return self._by_client.get(client_id)

    def delete(self, client_id: str) -> bool:
        """US-08: forget a client entirely. Returns whether anything was stored."""
        return self._by_client.pop(client_id, None) is not None

    def all(self) -> dict[str, ClientFeatures]:
        return dict(self._by_client)

    def clear(self) -> None:
        self._by_client.clear()
