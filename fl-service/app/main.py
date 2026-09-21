"""ASGI entrypoint (P50): what `uvicorn app.main:app` actually serves.

Deliberately tiny -- it wires one process-lifetime `ModelRegistry` (P49)
into `create_app` (`app/control/api.py`) and nothing else. The registry
starts at version 0 (an all-zero model, `zero_parameters()`) every time the
container starts; there is no persistence across restarts in this batch
(see `docker-compose.fl.yml`'s and V19's module docstrings for why
fl-service does not hold its own Postgres connection in this
implementation) -- a restarted fl-service simply serves a fresh v0 model
until the next round is run through it.
"""

from __future__ import annotations

from app.common.parameters import zero_parameters
from app.control.api import create_app
from app.server.model_registry import ModelRegistry

registry = ModelRegistry(zero_parameters())
app = create_app(registry)
