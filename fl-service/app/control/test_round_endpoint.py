from __future__ import annotations

import numpy as np
from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.common.schema import FEATURE_COUNT
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry
from app.server.strategy import DEFAULT_MIN_COHORT

def _app(store: ClientFeatureStore, demo: bool = False):
    return create_app(ModelRegistry(zero_parameters()), store, rng=np.random.default_rng(123), demo=demo)


def _put(store: ClientFeatureStore, client_id: str, seed: int, features: np.ndarray | None = None, examples: int = 6) -> None:
    rng = np.random.default_rng(seed)
    labels = np.array([i % 2 for i in range(examples)])
    store.put(client_id, 1, rng.normal(size=FEATURE_COUNT) if features is None else features, rng.uniform(size=(examples, FEATURE_COUNT)), labels)

def test_round_with_enough_clients_is_accepted_and_returns_one_entry_per_client() -> None:
    store = ClientFeatureStore()
    for i in range(DEFAULT_MIN_COHORT):
        _put(store, f"user-{i}", i)
    client = TestClient(_app(store))

    response = client.post("/fl/round", json={"round_number": 1})

    assert response.status_code == 200
    body = response.json()
    assert body["accepted"] is True
    assert body["cohort_size"] == DEFAULT_MIN_COHORT
    assert len(body["entries"]) == DEFAULT_MIN_COHORT

def test_round_below_min_cohort_is_refused_with_a_reason() -> None:
    store = ClientFeatureStore()
    _put(store, "only-user", 0)
    client = TestClient(_app(store))

    response = client.post("/fl/round", json={"round_number": 2})

    assert response.status_code == 200
    body = response.json()
    assert body["accepted"] is False
    assert "minimum cohort" in body["reason"]
    assert len(body["entries"]) == 1

def test_each_entry_carries_a_real_transmitted_vector_distinct_from_the_export() -> None:
    store = ClientFeatureStore()
    raw = np.random.default_rng(5).normal(size=FEATURE_COUNT)
    _put(store, "user-0", 5, features=raw)
    for i in range(1, DEFAULT_MIN_COHORT):
        _put(store, f"user-{i}", i)
    client = TestClient(_app(store))

    body = client.post("/fl/round", json={"round_number": 3}).json()

    entry = next(e for e in body["entries"] if e["client_id"] == "user-0")
    assert len(entry["transmitted_vector"]) == FEATURE_COUNT + 1
    assert entry["transmitted_vector"] != raw.tolist()
    assert entry["pre_clip_norm"] > 0
    assert entry["noise_multiplier"] > 0
    assert entry["epsilon"] > 0

def test_round_with_no_exported_clients_at_all_is_refused_with_zero_entries() -> None:
    client = TestClient(_app(ClientFeatureStore()))

    body = client.post("/fl/round", json={"round_number": 4}).json()

    assert body["accepted"] is False
    assert body["cohort_size"] == 0
    assert body["entries"] == []

def test_round_only_includes_the_client_ids_java_says_are_still_opted_in() -> None:
    """US-64: a client that withdrew after its last export still has a stale vector in the store; it must produce no update in later rounds."""
    store = ClientFeatureStore()
    for i in range(DEFAULT_MIN_COHORT + 1):
        _put(store, f"user-{i}", i)
    client = TestClient(_app(store))
    still_opted_in = [f"user-{i}" for i in range(1, DEFAULT_MIN_COHORT + 1)]

    body = client.post("/fl/round", json={"round_number": 5, "client_ids": still_opted_in}).json()

    assert body["cohort_size"] == DEFAULT_MIN_COHORT
    assert sorted(e["client_id"] for e in body["entries"]) == sorted(still_opted_in)
    assert "user-0" not in {e["client_id"] for e in body["entries"]}


def test_an_accepted_round_publishes_a_model_version_the_model_endpoint_serves() -> None:
    store = ClientFeatureStore()
    for i in range(DEFAULT_MIN_COHORT):
        _put(store, f"user-{i}", i)
    client = TestClient(_app(store))

    body = client.post("/fl/round", json={"round_number": 1}).json()

    assert body["model_version"] == 1
    model = client.get("/fl/model").json()
    assert model["version"] == 1
    assert any(abs(p) > 0 for p in model["parameters"])
    history = client.get("/fl/models").json()
    assert [v["version"] for v in history["versions"]] == [0, 1]
    assert len(history["feature_names"]) == FEATURE_COUNT


def test_a_client_without_enough_rated_rounds_sits_out() -> None:
    store = ClientFeatureStore()
    for i in range(DEFAULT_MIN_COHORT):
        _put(store, f"user-{i}", i)
    _put(store, "new-user", 9, examples=1)
    client = TestClient(_app(store))

    body = client.post("/fl/round", json={"round_number": 1}).json()

    assert body["sat_out_count"] == 1
    assert "new-user" not in {e["client_id"] for e in body["entries"]}


def test_demo_endpoints_and_noise_override_are_off_unless_enabled() -> None:
    client = TestClient(_app(ClientFeatureStore()))

    assert client.put("/fl/demo/synthetic-clients", json={"count": 5}).status_code == 404
    assert client.post("/fl/demo/reset").status_code == 404
    assert client.post("/fl/round", json={"round_number": 1, "noise_multiplier": 0.5}).status_code == 404


def test_demo_adds_synthetic_clients_runs_rounds_and_resets_to_version_zero() -> None:
    store = ClientFeatureStore()
    client = TestClient(_app(store, demo=True))

    assert client.put("/fl/demo/synthetic-clients", json={"count": 10, "seed": 3}).status_code == 200
    assert client.get("/fl/demo/status").json()["synthetic_clients"] == 10
    body = client.post("/fl/round", json={"round_number": 1, "client_ids": [], "noise_multiplier": 0.5}).json()
    assert body["accepted"] is True
    assert body["cohort_size"] == 10
    assert {e["noise_multiplier"] for e in body["entries"]} == {0.5}
    assert client.post("/fl/round", json={"round_number": 2, "noise_multiplier": 0.3}).status_code == 422

    assert client.post("/fl/demo/reset").status_code == 204
    assert client.get("/fl/model").json()["version"] == 0
    assert store.all() == {}

