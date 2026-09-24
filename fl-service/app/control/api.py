"""Thin FastAPI control wrapper (US-62, P49; extended P51/P54).

Deliberately small, per `fl-service/README.md`'s own framing ("Spring Boot
owns the whole application, and this directory exists only for the Flower
client/server processes and their scikit-learn training code"): a
distribution endpoint for the current global model (P49), a receiving
endpoint for Java's sanitised feature export (P51), and the feature schema
itself (P54) so both sides read the allow-list from one HTTP-reachable
place. No opt-in/opt-out here -- that is a user-account concept and lives
Java-side (P54), gating whether `FlFeatureExportJob` ever calls
`PUT /fl/client-features` for a given user in the first place.
"""

from __future__ import annotations

import numpy as np
from fastapi import Body, FastAPI, HTTPException
from pydantic import BaseModel, Field, field_validator

from app.common.schema import FEATURE_BOUNDS, FEATURE_COUNT
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry


class ClientFeaturesPayload(BaseModel):
    schema_version: int = Field(default=1)
    features: list[float] = Field(min_length=FEATURE_COUNT, max_length=FEATURE_COUNT)

    @field_validator("features")
    @classmethod
    def _exactly_the_schema_length(cls, value: list[float]) -> list[float]:
        if len(value) != FEATURE_COUNT:
            raise ValueError(f"expected exactly {FEATURE_COUNT} features, got {len(value)}")
        return value


def create_app(registry: ModelRegistry, client_store: ClientFeatureStore | None = None) -> FastAPI:
    """Builds a FastAPI app bound to one `ModelRegistry` (+ one
    `ClientFeatureStore`) instance. A factory rather than module-level
    singletons so tests can each use their own isolated state instead of
    sharing global state. `client_store` defaults to a fresh store so
    existing `create_app(registry)` callers (P49's tests) are unaffected.
    """
    app = FastAPI(title="Cohort FL control wrapper")
    store = client_store if client_store is not None else ClientFeatureStore()

    @app.get("/fl/model")
    def get_current_model() -> dict:
        current = registry.current
        return _serialise(current)

    @app.get("/fl/model/{version}")
    def get_model_version(version: int) -> dict:
        try:
            model_version = registry.get(version)
        except KeyError as exc:
            raise HTTPException(status_code=404, detail=str(exc)) from exc
        return _serialise(model_version)

    @app.get("/fl/schema")
    def get_schema() -> dict:
        """US-58/US-57: the exact allow-list shown on the opt-in consent
        screen, published verbatim rather than described in prose."""
        return {
            "schema_version": 1,
            "feature_count": FEATURE_COUNT,
            "features": [
                {"name": name, "lower_bound": bounds[0], "upper_bound": bounds[1]}
                for name, bounds in FEATURE_BOUNDS.items()
            ],
            "not_collected": [
                "free text", "names", "company names", "job titles", "dates/timestamps",
                "interviewer names", "resume content", "question text", "salary", "location",
            ],
        }

    @app.put("/fl/client-features/{client_id}")
    def put_client_features(client_id: str, payload: ClientFeaturesPayload = Body(...)) -> dict:
        """P51: Java's export job calls this once per opted-in user per
        export cycle. Always overwrites -- see `ClientFeatureStore.put`."""
        if payload.schema_version != 1:
            raise HTTPException(status_code=422, detail=f"unsupported schema_version {payload.schema_version}")
        store.put(client_id, payload.schema_version, np.array(payload.features, dtype=np.float64))
        return {"client_id": client_id, "schema_version": payload.schema_version, "feature_count": len(payload.features)}

    @app.get("/fl/client-features/{client_id}")
    def get_client_features(client_id: str) -> dict:
        record = store.get(client_id)
        if record is None:
            raise HTTPException(status_code=404, detail=f"no exported features for client {client_id!r}")
        return {
            "client_id": client_id,
            "schema_version": record.schema_version,
            "features": record.features.tolist(),
        }

    return app


def _serialise(model_version) -> dict:  # noqa: ANN001 -- ModelVersion, kept loose to avoid a circular import hint
    flat_parameters = [float(x) for array in model_version.parameters for x in array.reshape(-1)]
    return {
        "version": model_version.version,
        "round_number": model_version.round_number,
        "participant_count": model_version.participant_count,
        "created_at": model_version.created_at.isoformat(),
        "parameters": flat_parameters,
    }
