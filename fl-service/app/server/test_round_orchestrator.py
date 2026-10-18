from __future__ import annotations

import numpy as np

from app.client.local_trainer import train_local_round
from app.client.privacy import DEFAULT_CLIP_NORM, add_gaussian_noise, clip_update
from app.common.parameters import PARAMETER_COUNT, flatten, zero_parameters
from app.common.schema import FEATURE_COUNT
from app.control.client_store import ClientFeatures
from app.demo.synthetic import as_array, generate_synthetic_training_clients
from app.server.evaluation import check_set_accuracy
from app.server.model_registry import ModelRegistry
from app.server.round_orchestrator import run_training_round
from app.server.strategy import DEFAULT_MIN_COHORT


def _client(seed: int, examples: int = 8) -> ClientFeatures:
    rng = np.random.default_rng(seed)
    x = rng.uniform(size=(examples, FEATURE_COUNT))
    y = (rng.uniform(size=examples) < 0.5).astype(int)
    y[0], y[1] = 0, 1
    return ClientFeatures(schema_version=1, features=rng.uniform(size=FEATURE_COUNT), examples_x=x, examples_y=y)


def _registry() -> ModelRegistry:
    return ModelRegistry(zero_parameters())


def test_what_leaves_a_client_is_a_noised_model_update_not_its_features() -> None:
    client = _client(1)

    outcome = run_training_round({"user-1": client}, round_number=1, registry=_registry(), rng=np.random.default_rng(42))

    sent = np.array(outcome.entries[0].transmitted_vector)
    assert sent.shape == (PARAMETER_COUNT,)
    assert not np.allclose(sent[:FEATURE_COUNT], client.features)
    assert not any(np.allclose(sent[:FEATURE_COUNT], row) for row in client.examples_x)


def test_the_sent_update_is_exactly_the_trained_change_clipped_and_noised() -> None:
    client = _client(7)

    outcome = run_training_round({"user-1": client}, round_number=1, registry=_registry(), rng=np.random.default_rng(99))

    trained = train_local_round(zero_parameters(), client.examples_x, client.examples_y, seed=1000)
    delta = flatten(trained) - flatten(zero_parameters())
    expected_clipped = clip_update(delta, clip_norm=DEFAULT_CLIP_NORM)
    expected_sent = add_gaussian_noise(expected_clipped, np.random.default_rng(99), clip_norm=DEFAULT_CLIP_NORM)
    entry = outcome.entries[0]
    assert entry.transmitted_vector == expected_sent.tolist()
    assert entry.pre_clip_norm == float(np.linalg.norm(delta))
    assert entry.post_clip_norm == float(np.linalg.norm(expected_clipped))


def test_post_clip_norm_never_exceeds_the_clip_bound() -> None:
    clients = {f"user-{i}": _client(i, examples=40) for i in range(DEFAULT_MIN_COHORT)}

    outcome = run_training_round(clients, round_number=1, registry=_registry(), rng=np.random.default_rng(1))

    assert all(entry.post_clip_norm <= DEFAULT_CLIP_NORM + 1e-9 for entry in outcome.entries)


def test_a_round_below_min_cohort_is_refused_records_entries_and_keeps_the_model() -> None:
    registry = _registry()
    clients = {f"user-{i}": _client(i) for i in range(DEFAULT_MIN_COHORT - 1)}

    outcome = run_training_round(clients, round_number=5, registry=registry, rng=np.random.default_rng(1))

    assert outcome.accepted is False
    assert "minimum cohort" in (outcome.reason or "")
    assert outcome.cohort_size == DEFAULT_MIN_COHORT - 1
    assert len(outcome.entries) == DEFAULT_MIN_COHORT - 1
    assert outcome.model_version == 0
    assert registry.current.version == 0


def test_an_accepted_round_publishes_a_new_trained_version() -> None:
    registry = _registry()
    clients = {f"user-{i}": _client(i) for i in range(DEFAULT_MIN_COHORT)}

    outcome = run_training_round(clients, round_number=1, registry=registry, rng=np.random.default_rng(1))

    assert outcome.accepted is True
    assert outcome.model_version == 1
    assert registry.current.version == 1
    assert not np.allclose(flatten(registry.current.parameters), 0.0)
    assert registry.current.check_accuracy == outcome.check_accuracy


def test_clients_with_too_few_rated_rounds_sit_out_and_send_nothing() -> None:
    clients = {f"user-{i}": _client(i) for i in range(DEFAULT_MIN_COHORT)}
    clients["new-user"] = _client(9, examples=2)

    outcome = run_training_round(clients, round_number=1, registry=_registry(), rng=np.random.default_rng(1))

    assert outcome.sat_out_count == 1
    assert outcome.cohort_size == DEFAULT_MIN_COHORT
    assert "new-user" not in {entry.client_id for entry in outcome.entries}


def test_the_shared_model_gets_better_on_the_check_set_over_rounds() -> None:
    clients = {
        cid: ClientFeatures(schema_version=1, features=as_array(current), examples_x=x, examples_y=y)
        for cid, (current, x, y) in generate_synthetic_training_clients(40, seed=7).items()
    }
    registry = _registry()
    rng = np.random.default_rng(3)
    starting_accuracy = check_set_accuracy(registry.current.parameters)

    for round_number in range(1, 13):
        run_training_round(clients, round_number=round_number, registry=registry, rng=rng)

    assert registry.current.version == 12
    assert registry.current.check_accuracy is not None
    assert registry.current.check_accuracy > starting_accuracy + 0.05


def test_every_entry_has_a_positive_epsilon() -> None:
    outcome = run_training_round({"user-1": _client(3)}, round_number=1, registry=_registry(), rng=np.random.default_rng(1))
    assert outcome.entries[0].epsilon > 0
