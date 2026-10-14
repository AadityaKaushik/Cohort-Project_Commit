"""P55 (US-57): `GET /fl/schema` returns the allow-list as it is, so the consent screen can show what is sent."""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.common.schema import FEATURE_COUNT, FEATURE_NAMES
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry

def _client() -> TestClient:
    return TestClient(create_app(ModelRegistry(zero_parameters()), ClientFeatureStore()))

def test_schema_endpoint_publishes_exactly_the_24_feature_allow_list() -> None:
    response = _client().get("/fl/schema")

    assert response.status_code == 200
    body = response.json()
    assert body["feature_count"] == FEATURE_COUNT
    assert [f["name"] for f in body["features"]] == list(FEATURE_NAMES)
    for feature in body["features"]:
        assert isinstance(feature["lower_bound"], float)
        assert isinstance(feature["upper_bound"], float)

def test_schema_endpoint_lists_what_is_not_collected() -> None:
    body = _client().get("/fl/schema").json()
    assert "free text" in body["not_collected"]
    assert "resume content" in body["not_collected"]
