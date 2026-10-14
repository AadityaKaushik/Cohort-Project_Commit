from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from flwr.common import Code, FitRes, Status, ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_proxy import ClientProxy

from app.common.parameters import unflatten
from app.server.strategy import MinCohortFedAvg

class StubClientProxy(ClientProxy):
    def get_properties(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by MinCohortFedAvg.aggregate_fit")

    def get_parameters(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by MinCohortFedAvg.aggregate_fit")

    def fit(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by MinCohortFedAvg.aggregate_fit")

    def evaluate(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by MinCohortFedAvg.aggregate_fit")

    def reconnect(self, ins, timeout, group_id):  # noqa: D102, ANN001, ANN201
        raise NotImplementedError("unused by MinCohortFedAvg.aggregate_fit")

def build_fit_result(client_id: str, delta: np.ndarray, num_examples: int) -> tuple[ClientProxy, FitRes]:
    fit_res = FitRes(
        status=Status(code=Code.OK, message="ok"),
        parameters=ndarrays_to_parameters(unflatten(delta)),
        num_examples=num_examples,
        metrics={},
    )
    return StubClientProxy(cid=client_id), fit_res

@dataclass(frozen=True)
class RoundResult:
    accepted: bool
    parameters: list[np.ndarray]  # the active global model after this round
    reason: str | None
    participant_count: int
    filtered_count: int = 0  # how many participants' updates were dropped as anomalous (P48)

def run_round(
    strategy: MinCohortFedAvg,
    server_round: int,
    previous_parameters: list[np.ndarray],
    client_updates: list[tuple[str, np.ndarray, int]],
) -> RoundResult:
    strategy.previous_parameters = previous_parameters
    results = [build_fit_result(cid, delta, n) for cid, delta, n in client_updates]

    aggregated_parameters, metrics = strategy.aggregate_fit(server_round, results, [])

    if aggregated_parameters is None:
        return RoundResult(
            accepted=False,
            parameters=previous_parameters,
            reason=metrics.get("reason"),
            participant_count=int(metrics.get("participant_count", len(client_updates))),
            filtered_count=int(metrics.get("filtered_count", 0)),
        )

    return RoundResult(
        accepted=True,
        parameters=parameters_to_ndarrays(aggregated_parameters),
        reason=None,
        participant_count=int(metrics.get("participant_count", len(client_updates))),
        filtered_count=int(metrics.get("filtered_count", 0)),
    )
