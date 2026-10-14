from __future__ import annotations

import numpy as np
from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.common.schema import FEATURE_COUNT
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.server.model_registry import ModelRegistry
from app.server.strategy import DEFAULT_MIN_COHORT

def _app(store: ClientFeatureStore):
    return create_app(ModelRegistry(zero_parameters()), store, rng=np.random.default_rng(123))

def test_round_with_enough_clients_is_accepted_and_returns_one_entry_per_client() -> None:
    store = ClientFeatureStore()
    for i in range(DEFAULT_MIN_COHORT):
        store.put(f"user-{i}", 1, np.random.default_rng(i).normal(size=FEATURE_COUNT))
    client = TestClient(_app(store))

    response = client.post("/fl/round", json={"round_number": 1})

    assert response.status_code == 200
    body = response.json()
    assert body["accepted"] is True
    assert body["cohort_size"] == DEFAULT_MIN_COHORT
    assert len(body["entries"]) == DEFAULT_MIN_COHORT

def test_round_below_min_cohort_is_refused_with_a_reason() -> None:
    store = ClientFeatureStore()
    store.put("only-user", 1, np.zeros(FEATURE_COUNT))
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
    store.put("user-0", 1, raw)
    for i in range(1, DEFAULT_MIN_COHORT):
        store.put(f"user-{i}", 1, np.random.default_rng(i).normal(size=FEATURE_COUNT))
    client = TestClient(_app(store))

    body = client.post("/fl/round", json={"round_number": 3}).json()

    entry = next(e for e in body["entries"] if e["client_id"] == "user-0")
    assert len(entry["transmitted_vector"]) == FEATURE_COUNT
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
        store.put(f"user-{i}", 1, np.random.default_rng(i).normal(size=FEATURE_COUNT))
    client = TestClient(_app(store))
    still_opted_in = [f"user-{i}" for i in range(1, DEFAULT_MIN_COHORT + 1)]

    body = client.post("/fl/round", json={"round_number": 5, "client_ids": still_opted_in}).json()

    assert body["cohort_size"] == DEFAULT_MIN_COHORT
    assert sorted(e["client_id"] for e in body["entries"]) == sorted(still_opted_in)
    assert "user-0" not in {e["client_id"] for e in body["entries"]}
