"""P32's mandatory verify (BUILD_PHASES_JAVA.md): proves the spike actually
executes and its FedAvg round does what a FedAvg round is supposed to do,
rather than relying on eyeballing printed output."""

from spike.run import run_spike


def test_federated_model_beats_every_clients_local_only_model():
    results = run_spike()

    best_local_only = max(results["local_only_client0_accuracy"], results["local_only_client1_accuracy"])

    assert results["best_federated_accuracy"] >= best_local_only
    # Sanity bound: on this synthetic, well-separated dataset the federated
    # model should be clearly better than chance (0.5), not just technically higher.
    assert results["best_federated_accuracy"] > 0.75


def test_run_is_deterministic_under_the_fixed_seed():
    first = run_spike()
    second = run_spike()

    assert first == second
