"""A thin, in-process driver for one FL round (P47).

Per this batch's environment constraints (no Docker, no real Flower
client-server transport -- see `app/server/strategy.py`'s module docstring
and ADR-011), rounds are driven by direct function calls rather than real
networking, the same technique the P32 spike used to exercise Flower's
*real* `aggregate_fit`/`Parameters` types without needing a live
client-server connection (confirmed safe by reading `Strategy.aggregate_fit`'s
contract: implementations only ever read from `results`/`failures`, never
call back into the `ClientProxy` objects that key them).

`run_round` is the seam that turns a strategy's Flower-shaped return
(`(Parameters | None, metrics)`) into the simple "what is the active global
model now" answer the rest of Sprint 4 (P49's model registry) needs: on
acceptance, the newly aggregated parameters; on refusal, the *same*
previous parameters, unchanged -- refusal never mutates or discards the
last known good model.
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from flwr.common import Code, FitRes, Status, ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_proxy import ClientProxy

from app.common.parameters import unflatten
from app.server.strategy import MinCohortFedAvg


class StubClientProxy(ClientProxy):
    """A placeholder `ClientProxy`. `Strategy.aggregate_fit` implementations
    only use the proxy as an (ignored) tuple key -- confirmed by reading
    `Strategy`'s contract before relying on this, the same check the P32
    spike made for `FedAvg.aggregate_fit`. A real `ClientProxy` talks to a
    client over the network; nothing in this phase makes that network hop.
    """

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
    """One client's already-privatized (clipped + noised) delta, wrapped as
    the `(ClientProxy, FitRes)` pair `aggregate_fit` expects. `delta` is the
    flat 25-float vector `app/client/privacy.py` produces."""
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


def run_round(
    strategy: MinCohortFedAvg,
    server_round: int,
    previous_parameters: list[np.ndarray],
    client_updates: list[tuple[str, np.ndarray, int]],
) -> RoundResult:
    """Runs one round: wraps each `(client_id, delta, num_examples)` triple
    as a Flower fit result, calls the strategy's `aggregate_fit`, and
    resolves its Flower-shaped return into the model that is active
    afterwards -- the new one if accepted, `previous_parameters` unchanged
    if refused.
    """
    strategy.previous_parameters = previous_parameters
    results = [build_fit_result(cid, delta, n) for cid, delta, n in client_updates]

    aggregated_parameters, metrics = strategy.aggregate_fit(server_round, results, [])

    if aggregated_parameters is None:
        return RoundResult(
            accepted=False,
            parameters=previous_parameters,
            reason=metrics.get("reason"),
            participant_count=int(metrics.get("participant_count", len(client_updates))),
        )

    return RoundResult(
        accepted=True,
        parameters=parameters_to_ndarrays(aggregated_parameters),
        reason=None,
        participant_count=int(metrics.get("participant_count", len(client_updates))),
    )
