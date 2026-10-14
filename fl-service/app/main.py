from __future__ import annotations

from app.common.parameters import zero_parameters
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry

registry = ModelRegistry(zero_parameters())
client_store = ClientFeatureStore()
app = create_app(registry, client_store)
