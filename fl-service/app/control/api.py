from __future__ import annotations

import os

import numpy as np
from fastapi import Body, FastAPI, HTTPException, Response
from pydantic import BaseModel, Field, field_validator

from app.client.privacy import DEFAULT_NOISE_MULTIPLIER
from app.common.schema import FEATURE_BOUNDS, FEATURE_COUNT, FEATURE_NAMES
from app.control.client_store import ClientFeatureStore
from app.demo.synthetic import as_array, generate_synthetic_training_clients, is_synthetic
from app.server.evaluation import central_ceiling
from app.server.model_registry import ModelRegistry
from app.server.round_orchestrator import RoundOutcome, run_training_round

# Lower noise is a demo-only option, to show what privacy costs in accuracy. Scheduled rounds always use the default.
DEMO_NOISE_LEVELS = (1.0, 0.5, 0.25)


class TrainingExample(BaseModel):
    features: list[float] = Field(min_length=FEATURE_COUNT, max_length=FEATURE_COUNT)
    label: int = Field(ge=0, le=1)


class ClientFeaturesPayload(BaseModel):
    schema_version: int = Field(default=1)
    features: list[float] = Field(min_length=FEATURE_COUNT, max_length=FEATURE_COUNT)
    examples: list[TrainingExample] = Field(default_factory=list, max_length=500)

    @field_validator("features")
    @classmethod
    def _exactly_the_schema_length(cls, value: list[float]) -> list[float]:
        if len(value) != FEATURE_COUNT:
            raise ValueError(f"expected exactly {FEATURE_COUNT} features, got {len(value)}")
        return value


class RoundRequest(BaseModel):
    round_number: int = Field(ge=1)
    client_ids: list[str] | None = None
    noise_multiplier: float | None = None


class SyntheticClientsRequest(BaseModel):
    count: int = Field(ge=0, le=200)
    seed: int = 7


def demo_enabled() -> bool:
    return os.environ.get("FL_DEMO_ENABLED", "false").lower() == "true"


def create_app(
    registry: ModelRegistry,
    client_store: ClientFeatureStore | None = None,
    rng: np.random.Generator | None = None,
    demo: bool | None = None,
) -> FastAPI:
    app = FastAPI(title="Cohort FL control wrapper")
    store = client_store if client_store is not None else ClientFeatureStore()
    noise_rng = rng if rng is not None else np.random.default_rng()
    demo_on = demo_enabled() if demo is None else demo

    def require_demo() -> None:
        if not demo_on:
            raise HTTPException(status_code=404, detail="demo endpoints are off (set FL_DEMO_ENABLED=true)")

    @app.get("/fl/model")
    def get_current_model() -> dict:
        return _serialise(registry.current)

    @app.get("/fl/models")
    def list_models() -> dict:
        return {
            "feature_names": list(FEATURE_NAMES),
            "central_ceiling": central_ceiling() if demo_on else None,
            "versions": [_serialise(version) for version in registry.history],
        }

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
        examples_x = np.array([example.features for example in payload.examples], dtype=np.float64).reshape(-1, FEATURE_COUNT)
        examples_y = np.array([example.label for example in payload.examples], dtype=int)
        store.put(client_id, payload.schema_version, np.array(payload.features, dtype=np.float64), examples_x, examples_y)
        return {
            "client_id": client_id,
            "schema_version": payload.schema_version,
            "feature_count": len(payload.features),
            "example_count": len(payload.examples),
        }

    @app.get("/fl/client-features/{client_id}")
    def get_client_features(client_id: str) -> dict:
        record = store.get(client_id)
        if record is None:
            raise HTTPException(status_code=404, detail=f"no exported features for client {client_id!r}")
        return {
            "client_id": client_id,
            "schema_version": record.schema_version,
            "features": record.features.tolist(),
            "example_count": record.example_count,
        }

    @app.delete("/fl/client-features/{client_id}", status_code=204)
    def delete_client_features(client_id: str) -> Response:
        """US-08: account deletion removes this client's exported state."""
        store.delete(client_id)
        return Response(status_code=204)

    @app.post("/fl/round")
    def run_round(request: RoundRequest = Body(...)) -> dict:
        noise = DEFAULT_NOISE_MULTIPLIER
        if request.noise_multiplier is not None:
            require_demo()
            if request.noise_multiplier not in DEMO_NOISE_LEVELS:
                raise HTTPException(status_code=422, detail=f"noise_multiplier must be one of {DEMO_NOISE_LEVELS}")
            noise = request.noise_multiplier
        stored = store.all()
        if request.client_ids is not None:
            wanted = set(request.client_ids)
            # Synthetic clients only exist after someone adds them by hand for a demo.
            stored = {cid: rec for cid, rec in stored.items() if cid in wanted or is_synthetic(cid)}
        outcome = run_training_round(stored, round_number=request.round_number, registry=registry, rng=noise_rng, noise_multiplier=noise)
        return _serialise_round(outcome)

    @app.put("/fl/demo/synthetic-clients")
    def set_synthetic_clients(request: SyntheticClientsRequest = Body(...)) -> dict:
        require_demo()
        _remove_synthetic(store)
        for client_id, (current, x, y) in generate_synthetic_training_clients(request.count, request.seed).items():
            store.put(client_id, 1, as_array(current), x, y)
        return {"synthetic_clients": request.count}

    @app.delete("/fl/demo/synthetic-clients", status_code=204)
    def remove_synthetic_clients() -> Response:
        require_demo()
        _remove_synthetic(store)
        return Response(status_code=204)

    @app.post("/fl/demo/reset", status_code=204)
    def reset_demo() -> Response:
        """Removes synthetic clients and puts the shared model back to version 0."""
        require_demo()
        _remove_synthetic(store)
        registry.reset()
        return Response(status_code=204)

    @app.get("/fl/demo/status")
    def demo_status() -> dict:
        require_demo()
        clients = store.all()
        synthetic = [record for cid, record in clients.items() if is_synthetic(cid)]
        return {
            "synthetic_clients": len(synthetic),
            "real_clients": len(clients) - len(synthetic),
            "noise_levels": list(DEMO_NOISE_LEVELS),
        }

    return app


def _remove_synthetic(store: ClientFeatureStore) -> None:
    for client_id in [cid for cid in store.all() if is_synthetic(cid)]:
        store.delete(client_id)


def _serialise_round(outcome: RoundOutcome) -> dict:
    return {
        "round_number": outcome.round_number,
        "accepted": outcome.accepted,
        "reason": outcome.reason,
        "cohort_size": outcome.cohort_size,
        "sat_out_count": outcome.sat_out_count,
        "filtered_count": outcome.filtered_count,
        "model_version": outcome.model_version,
        "check_accuracy": outcome.check_accuracy,
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
        "check_accuracy": model_version.check_accuracy,
        "parameters": flat_parameters,
    }
