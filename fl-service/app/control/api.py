from __future__ import annotations

import numpy as np
from fastapi import Body, FastAPI, HTTPException, Response
from pydantic import BaseModel, Field, field_validator

from app.common.schema import FEATURE_BOUNDS, FEATURE_COUNT
from app.control.client_store import ClientFeatureStore
from app.demo.synthetic import is_synthetic
from app.server.model_registry import ModelRegistry
from app.server.round_orchestrator import RoundOutcome, run_privacy_round

class ClientFeaturesPayload(BaseModel):
    schema_version: int = Field(default=1)
    features: list[float] = Field(min_length=FEATURE_COUNT, max_length=FEATURE_COUNT)

    @field_validator("features")
    @classmethod
    def _exactly_the_schema_length(cls, value: list[float]) -> list[float]:
        if len(value) != FEATURE_COUNT:
            raise ValueError(f"expected exactly {FEATURE_COUNT} features, got {len(value)}")
        return value

class RoundRequest(BaseModel):
    round_number: int = Field(ge=1)
    client_ids: list[str] | None = None

def create_app(
    registry: ModelRegistry,
    client_store: ClientFeatureStore | None = None,
    rng: np.random.Generator | None = None,
) -> FastAPI:
    app = FastAPI(title="Cohort FL control wrapper")
    store = client_store if client_store is not None else ClientFeatureStore()
    noise_rng = rng if rng is not None else np.random.default_rng()

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

    @app.delete("/fl/client-features/{client_id}", status_code=204)
    def delete_client_features(client_id: str) -> Response:
        """US-08: account deletion removes this client's exported state."""
        store.delete(client_id)
        return Response(status_code=204)

    @app.post("/fl/round")
    def run_round(request: RoundRequest = Body(...)) -> dict:
        stored = store.all()
        if request.client_ids is not None:
            wanted = set(request.client_ids)
            # Synthetic clients only exist after someone runs app.demo.simulate by hand.
            stored = {cid: rec for cid, rec in stored.items() if cid in wanted or is_synthetic(cid)}
        features_by_client = {cid: (rec.schema_version, rec.features) for cid, rec in stored.items()}
        outcome = run_privacy_round(features_by_client, round_number=request.round_number, rng=noise_rng)
        return _serialise_round(outcome)

    return app

def _serialise_round(outcome: RoundOutcome) -> dict:
    return {
        "round_number": outcome.round_number,
        "accepted": outcome.accepted,
        "reason": outcome.reason,
        "cohort_size": outcome.cohort_size,
        "entries": [
            {
                "client_id": entry.client_id,
                "schema_version": entry.schema_version,
                "pre_clip_norm": entry.pre_clip_norm,
                "post_clip_norm": entry.post_clip_norm,
                "noise_multiplier": entry.noise_multiplier,
                "epsilon": entry.epsilon,
                "transmitted_vector": entry.transmitted_vector,
            }
            for entry in outcome.entries
        ],
    }

def _serialise(model_version) -> dict:  # noqa: ANN001
    flat_parameters = [float(x) for array in model_version.parameters for x in array.reshape(-1)]
    return {
        "version": model_version.version,
        "round_number": model_version.round_number,
        "participant_count": model_version.participant_count,
        "created_at": model_version.created_at.isoformat(),
        "parameters": flat_parameters,
    }
