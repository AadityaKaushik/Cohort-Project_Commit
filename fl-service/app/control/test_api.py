from __future__ import annotations

import numpy as np
from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.control.api import create_app
from app.server.model_registry import ModelRegistry, run_and_publish_round
from app.server.strategy import MinCohortFedAvg

def test_get_current_model_returns_version_zero_initially() -> None:
    registry = ModelRegistry(zero_parameters())
    client = TestClient(create_app(registry))

    response = client.get("/fl/model")

    assert response.status_code == 200
    body = response.json()
    assert body["version"] == 0
    assert len(body["parameters"]) == 25

def test_get_current_model_reflects_a_published_round() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)
    client_updates = [(f"c{i}", np.full(25, 0.04), 5) for i in range(3)]
    run_and_publish_round(registry, strategy, server_round=1, client_updates=client_updates)

    client = TestClient(create_app(registry))
    body = client.get("/fl/model").json()

    assert body["version"] == 1
    assert body["round_number"] == 1
    assert body["participant_count"] == 3

def test_a_refused_round_leaves_the_endpoint_serving_the_previous_version() -> None:
    registry = ModelRegistry(zero_parameters())
    strategy = MinCohortFedAvg(min_cohort=3)

    good_updates = [(f"c{i}", np.full(25, 0.04), 5) for i in range(3)]
    run_and_publish_round(registry, strategy, server_round=1, client_updates=good_updates)

    client = TestClient(create_app(registry))
    before = client.get("/fl/model").json()

    too_few_updates = [("a", np.full(25, 0.05), 5), ("b", np.full(25, 0.05), 5)]
    result = run_and_publish_round(registry, strategy, server_round=2, client_updates=too_few_updates)
    assert result.accepted is False

    after = client.get("/fl/model").json()
    assert after == before
    assert after["version"] == 1

def test_get_a_specific_version_by_number() -> None:
    registry = ModelRegistry(zero_parameters())
    client = TestClient(create_app(registry))

    response = client.get("/fl/model/0")

    assert response.status_code == 200
    assert response.json()["version"] == 0

def test_get_an_unknown_version_returns_404() -> None:
    registry = ModelRegistry(zero_parameters())
    client = TestClient(create_app(registry))

    response = client.get("/fl/model/42")

    assert response.status_code == 404
