"""P33: the spike runs and its FedAvg round beats the local-only models."""

from spike.run import run_spike


def test_federated_model_beats_every_clients_local_only_model():
    results = run_spike()

    best_local_only = max(results["local_only_client0_accuracy"], results["local_only_client1_accuracy"])

    assert results["best_federated_accuracy"] >= best_local_only
    # Well above chance on this synthetic data.
    assert results["best_federated_accuracy"] > 0.75


def test_run_is_deterministic_under_the_fixed_seed():
    first = run_spike()
    second = run_spike()

    assert first == second
