"""US-58: `PUT /fl/client-features/{client_id}` accepts exactly 24 floats matching the schema."""

from __future__ import annotations

import numpy as np
from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.common.schema import FEATURE_COUNT
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry

def _client() -> TestClient:
    return TestClient(create_app(ModelRegistry(zero_parameters()), ClientFeatureStore()))

def test_put_client_features_accepts_exactly_24_floats() -> None:
    client = _client()
    response = client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * FEATURE_COUNT})
    assert response.status_code == 200
    assert response.json() == {"client_id": "user-1", "schema_version": 1, "feature_count": FEATURE_COUNT, "example_count": 0}

def test_put_client_features_rejects_wrong_length() -> None:
    client = _client()
    response = client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * (FEATURE_COUNT - 1)})
    assert response.status_code == 422

def test_put_client_features_rejects_unsupported_schema_version() -> None:
    client = _client()
    response = client.put("/fl/client-features/user-1", json={"schema_version": 2, "features": [0.1] * FEATURE_COUNT})
    assert response.status_code == 422

def test_put_then_get_round_trips_the_same_vector() -> None:
    client = _client()
    vector = [i / 100 for i in range(FEATURE_COUNT)]
    client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": vector})

    response = client.get("/fl/client-features/user-1")

    assert response.status_code == 200
    assert response.json()["features"] == vector

def test_get_client_features_for_unknown_client_is_404() -> None:
    response = _client().get("/fl/client-features/never-exported")
    assert response.status_code == 404

def test_put_overwrites_a_prior_export_for_the_same_client() -> None:
    client = _client()
    client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * FEATURE_COUNT})
    client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.9] * FEATURE_COUNT})

    response = client.get("/fl/client-features/user-1")

    assert response.json()["features"] == [0.9] * FEATURE_COUNT

def test_client_feature_store_all_reflects_every_put() -> None:
    store = ClientFeatureStore()
    store.put("a", 1, np.zeros(FEATURE_COUNT))
    store.put("b", 1, np.ones(FEATURE_COUNT))
    assert set(store.all().keys()) == {"a", "b"}

def test_delete_client_features_forgets_the_client_and_is_idempotent() -> None:
    client = _client()
    client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * FEATURE_COUNT})
    client.put("/fl/client-features/user-2", json={"schema_version": 1, "features": [0.2] * FEATURE_COUNT})

    assert client.delete("/fl/client-features/user-1").status_code == 204
    assert client.get("/fl/client-features/user-1").status_code == 404
    assert client.get("/fl/client-features/user-2").status_code == 200
    assert client.delete("/fl/client-features/user-1").status_code == 204
    assert client.delete("/fl/client-features/never-exported").status_code == 204

def test_a_deleted_client_is_excluded_from_the_next_round() -> None:
    store = ClientFeatureStore()
    store.put("gone", 1, np.zeros(FEATURE_COUNT))
    assert store.delete("gone") is True
    assert store.delete("gone") is False
    assert "gone" not in store.all()


def test_put_client_features_stores_labelled_examples() -> None:
    store = ClientFeatureStore()
    client = TestClient(create_app(ModelRegistry(zero_parameters()), store))
    examples = [{"features": [0.2] * FEATURE_COUNT, "label": 1}, {"features": [0.7] * FEATURE_COUNT, "label": 0}]

    response = client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * FEATURE_COUNT, "examples": examples})

    assert response.json()["example_count"] == 2
    record = store.get("user-1")
    assert record is not None
    assert record.examples_y.tolist() == [1, 0]
    assert record.examples_x.shape == (2, FEATURE_COUNT)


def test_an_example_label_must_be_zero_or_one() -> None:
    client = TestClient(create_app(ModelRegistry(zero_parameters())))
    bad = [{"features": [0.2] * FEATURE_COUNT, "label": 4}]

    response = client.put("/fl/client-features/user-1", json={"schema_version": 1, "features": [0.1] * FEATURE_COUNT, "examples": bad})

    assert response.status_code == 422

