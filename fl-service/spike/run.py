"""P32 spike: does a two-client FedAvg round work end to end with a
scikit-learn model, using Flower's *real* parameter/aggregation code?

This deliberately drives `flwr.server.strategy.FedAvg.aggregate_fit` and
`flwr.common.{ndarrays_to_parameters,parameters_to_ndarrays}` by hand,
in-process, rather than through Flower's client/server networking or
simulation runtime. See `docs/adr/ADR-011-fl-spike-findings.md` for why:
in short, flwr 1.37's public API has moved the networked/simulated path onto
a much larger surface (SuperLink/SuperNode processes, or a Ray-backed
`run_simulation`) than the `start_server`/`start_client` pattern most
tutorials still show, and de-risking *that* is a separate, larger piece of
work than a 1-day spike affords. What this script still proves for real,
using Flower's own (non-deprecated) types throughout:

1. Two independently-trained local models' parameters really do serialise
   through `Parameters`/`FitRes` and back into plain ndarrays.
2. `FedAvg.aggregate_fit` -- the exact strategy class Sprint 4 will use --
   produces a global model that outperforms either client's local-only model
   on held-out data it never trained on, which is FedAvg's entire premise.
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
    """A placeholder `ClientProxy`: `FedAvg.aggregate_fit` only uses the proxy
    as an (ignored) tuple key, never calls a method on it -- confirmed by
    reading its source before relying on this. A real `ClientProxy` talks to
    a client over the network; this spike has no network hop to make."""

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

    # Round 0 baseline: each client's model, trained only on its own shard,
    # evaluated on the shared held-out set -- this is what "no federation"
    # looks like, and the number FedAvg has to beat.
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
