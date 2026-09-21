"""Thin FastAPI control wrapper (US-62, P49).

Deliberately small, per `fl-service/README.md`'s own framing ("Spring Boot
owns the whole application, and this directory exists only for the Flower
client/server processes and their scikit-learn training code"): this is not
a general API, just a distribution endpoint for the current global model so
a caller (eventually the Java-side `FlowerServiceAdapter`, P52 -- out of
scope here) can fetch what to serve. No opt-in/opt-out, no privacy ledger,
no `/fl/schema` -- those are P54+/P55+ scope.
"""

from __future__ import annotations

from fastapi import FastAPI, HTTPException

from app.server.model_registry import ModelRegistry


def create_app(registry: ModelRegistry) -> FastAPI:
    """Builds a FastAPI app bound to one `ModelRegistry` instance. A factory
    rather than a module-level singleton so tests can each use their own
    isolated registry instead of sharing global state.
    """
    app = FastAPI(title="Cohort FL control wrapper")

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
