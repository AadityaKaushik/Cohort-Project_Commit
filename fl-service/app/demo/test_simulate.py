from __future__ import annotations

import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.common.parameters import zero_parameters
from app.common.parameters import PARAMETER_COUNT
from app.common.schema import FEATURE_COUNT
from app.control.api import create_app
from app.control.client_store import ClientFeatureStore
from app.demo.simulate import main, run_demo
from app.demo.synthetic import is_synthetic
from app.server.model_registry import ModelRegistry
from app.server.strategy import DEFAULT_MIN_COHORT

FL_URL = "http://fl"
API_URL = "http://api"
REAL_USER = "deadbeef-0001-4000-8000-000000000001"

class FakeStack:
    """fl-service runs for real through TestClient; the Java side is reduced to what PrivacyLedgerService does."""

    def __init__(self, opted_in: list[str]) -> None:
        self.store = ClientFeatureStore()
        self.fl = TestClient(create_app(ModelRegistry(zero_parameters()), self.store, rng=np.random.default_rng(0)))
        self.opted_in = opted_in
        self.ledger: list[dict] = []
        self.round_responses: list[dict] = []

    def __call__(self, method: str, url: str, body: dict | None = None, token: str | None = None) -> dict | None:
        if url.startswith(FL_URL):
            response = self.fl.request(method, url.removeprefix(FL_URL), json=body)
            assert response.status_code < 300, response.text
            return response.json() if response.content else None
        if url == f"{API_URL}/api/v1/auth/login":
            return {"access_token": "token"}
        if url == f"{API_URL}/api/v1/dev/fl/round":
            assert token == "token"
            return self._java_round()
        raise AssertionError(f"unexpected call {method} {url}")

    def _java_round(self) -> dict:
        for user_id in self.opted_in:
            rated_rounds = np.random.default_rng(1).uniform(size=(8, FEATURE_COUNT))
            self.store.put(user_id, 1, np.full(FEATURE_COUNT, 0.5), rated_rounds, np.array([1, 0, 1, 1, 0, 0, 1, 0]))
        if not self.opted_in:
            return {"exported_users": 0, "export_failures": 0, "round_ran": False}
        round_number = len(self.round_responses) + 1
        body = self.fl.post("/fl/round", json={"round_number": round_number, "client_ids": self.opted_in}).json()
        self.round_responses.append(body)
        self.ledger.extend(entry for entry in body["entries"] if entry["client_id"] in self.opted_in)
        return {
            "exported_users": len(self.opted_in),
            "export_failures": 0,
            "round_ran": True,
            "round_number": body["round_number"],
            "accepted": body["accepted"],
            "reason": body["reason"],
            "cohort_size": body["cohort_size"],
            "model_version": body["model_version"],
        }

def _run(stack: FakeStack, clients: int, rounds: int = 1) -> list[dict]:
    return run_demo(stack, FL_URL, API_URL, "seed.demo.user@example.com", "pw", clients, rounds, seed=7, log=lambda _: None)

def test_round_with_enough_synthetic_clients_is_accepted_and_the_real_user_gets_a_ledger_entry() -> None:
    stack = FakeStack(opted_in=[REAL_USER])

    results = _run(stack, clients=8, rounds=2)

    assert [r["accepted"] for r in results] == [True, True]
    assert [r["cohort_size"] for r in results] == [9, 9]
    assert len(stack.ledger) == 2
    assert all(entry["client_id"] == REAL_USER for entry in stack.ledger)
    assert all(len(entry["transmitted_vector"]) == PARAMETER_COUNT for entry in stack.ledger)
    assert [r["model_version"] for r in results] == [1, 2]

def test_round_below_the_minimum_cohort_is_still_refused() -> None:
    stack = FakeStack(opted_in=[REAL_USER])

    results = _run(stack, clients=DEFAULT_MIN_COHORT - 2)

    assert results[0]["accepted"] is False
    assert "minimum cohort" in results[0]["reason"]
    assert results[0]["cohort_size"] == DEFAULT_MIN_COHORT - 1

def test_synthetic_entries_are_marked_and_never_reach_the_ledger() -> None:
    stack = FakeStack(opted_in=[REAL_USER])

    _run(stack, clients=4)

    returned_ids = [entry["client_id"] for entry in stack.round_responses[0]["entries"]]
    assert sorted(i for i in returned_ids if i != REAL_USER) == [f"synthetic-client-0{i}" for i in range(1, 5)]
    assert all(is_synthetic(i) for i in returned_ids if i != REAL_USER)
    assert not any(is_synthetic(entry["client_id"]) for entry in stack.ledger)

def test_synthetic_clients_are_removed_after_the_demo() -> None:
    stack = FakeStack(opted_in=[REAL_USER])

    _run(stack, clients=5)

    assert list(stack.store.all()) == [REAL_USER]

def test_no_round_runs_when_nobody_is_opted_in() -> None:
    stack = FakeStack(opted_in=[])

    results = _run(stack, clients=8)

    assert results == [{"exported_users": 0, "export_failures": 0, "round_ran": False}]
    assert stack.store.all() == {}

def test_scheduled_rounds_without_the_demo_ignore_nothing_new() -> None:
    stack = FakeStack(opted_in=[REAL_USER])
    rated = np.random.default_rng(2).uniform(size=(6, FEATURE_COUNT))
    labels = np.array([1, 0, 1, 0, 1, 0])
    stack.store.put("another-real-user", 1, np.zeros(FEATURE_COUNT), rated, labels)
    stack.store.put(REAL_USER, 1, np.zeros(FEATURE_COUNT), rated, labels)

    body = stack.fl.post("/fl/round", json={"round_number": 1, "client_ids": [REAL_USER]}).json()

    assert body["cohort_size"] == 1
    assert body["accepted"] is False

def test_main_needs_the_password_in_the_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("COHORT_DEMO_PASSWORD", raising=False)

    assert main(["--clients", "8"]) == 2
