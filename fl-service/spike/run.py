"""P33 spike: a two-client FedAvg round with scikit-learn models and Flower's own aggregation code.

It calls `FedAvg.aggregate_fit` and the parameter conversion helpers directly, in
process, instead of going through Flower's client/server runtime. The reasons are
in docs/adr/ADR-011-fl-spike-findings.md.

It shows that:
1. Two locally trained models serialise through `Parameters`/`FitRes` and back to ndarrays.
2. `FedAvg.aggregate_fit` gives a global model that beats either local-only model on held-out data.
"""

from __future__ import annotations

import sys

import numpy as np
from flwr.common import Code, FitRes, Status, ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_proxy import ClientProxy
from flwr.server.strategy import FedAvg

from spike.client import SklearnFedAvgClient, new_local_model
from spike.data import build_two_client_dataset

N_ROUNDS = 5


class StubClientProxy(ClientProxy):
    """`FedAvg.aggregate_fit` only uses the proxy as a dict key, so no methods are needed."""

    def get_properties(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by FedAvg.aggregate_fit")

    def get_parameters(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by FedAvg.aggregate_fit")

    def fit(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by FedAvg.aggregate_fit")

    def evaluate(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by FedAvg.aggregate_fit")

    def reconnect(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by FedAvg.aggregate_fit")


def score(parameters: list[np.ndarray], x: np.ndarray, y: np.ndarray) -> float:
    model = new_local_model()
    model.partial_fit(x[:1], y[:1], classes=np.array([0, 1]))
    model.coef_, model.intercept_ = parameters[0].copy(), parameters[1].copy()
    return float(model.score(x, y))


def run_spike() -> dict[str, float]:
    dataset = build_two_client_dataset()
    clients = [SklearnFedAvgClient(shard) for shard in dataset.client_shards]
    strategy = FedAvg(min_fit_clients=2, min_available_clients=2)

    # Baseline without federation: the number FedAvg has to beat.
    local_only_accuracies = [
        score(client.get_parameters({}), dataset.x_test, dataset.y_test) for client in clients
    ]

    global_parameters = clients[0].get_parameters({})
    round_accuracies: list[float] = []

    for server_round in range(1, N_ROUNDS + 1):
        results: list[tuple[ClientProxy, FitRes]] = []
        for i, client in enumerate(clients):
            new_params, num_examples, metrics = client.fit(global_parameters, {})
            fit_res = FitRes(
                status=Status(code=Code.OK, message="ok"),
                parameters=ndarrays_to_parameters(new_params),
                num_examples=num_examples,
                metrics=metrics,
            )
            results.append((StubClientProxy(cid=str(i)), fit_res))

        aggregated_parameters, _ = strategy.aggregate_fit(server_round, results, [])
        if aggregated_parameters is None:
            raise RuntimeError(f"round {server_round}: FedAvg.aggregate_fit returned no parameters")
        global_parameters = parameters_to_ndarrays(aggregated_parameters)

        accuracy = score(global_parameters, dataset.x_test, dataset.y_test)
        round_accuracies.append(accuracy)
        print(f"round {server_round}: global model held-out accuracy = {accuracy:.4f}")

    return {
        "local_only_client0_accuracy": local_only_accuracies[0],
        "local_only_client1_accuracy": local_only_accuracies[1],
        "final_federated_accuracy": round_accuracies[-1],
        "best_federated_accuracy": max(round_accuracies),
    }


def main() -> int:
    results = run_spike()
    print("\n--- summary ---")
    for key, value in results.items():
        print(f"{key}: {value:.4f}")

    best_local_only = max(results["local_only_client0_accuracy"], results["local_only_client1_accuracy"])
    if results["best_federated_accuracy"] < best_local_only:
        print(
            "\nFAIL: the federated model never beat the better client's local-only model "
            f"({results['best_federated_accuracy']:.4f} < {best_local_only:.4f})."
        )
        return 1

    print("\nOK: FedAvg's global model outperformed every client's local-only model on held-out data.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
