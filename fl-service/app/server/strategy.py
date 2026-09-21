"""Flower FedAvg strategy with minimum-cohort refusal (US-61, P47).

Per ADR-011's flagged open decision ("the client-server transport choice...
should be made deliberately... at the start of P47"): this implementation
drives Flower's `Strategy` class directly -- the same real, non-deprecated
aggregation surface the P32 spike validated (`aggregate_fit`,
`ndarrays_to_parameters`/`parameters_to_ndarrays`) -- rather than either of
ADR-011's two networked options (pinning to the legacy `start_server`/
`start_client` API, or adopting the newer SuperLink/SuperNode + ClientApp/
ServerApp architecture). Real gRPC transport is out of scope for P43-P49
(it is explicitly deferred to the docker-compose `fl` profile, P50+); this
class is fully usable as a real Flower `Strategy` if that transport is
added later, since nothing here depends on how `results` got assembled.

Two things this strategy deliberately does differently from vanilla
`flwr.server.strategy.FedAvg`, both required by PROJECT_PLAN.md §22.4:

1. **It averages *deltas*, not absolute parameters, uniformly weighted.**
   Standard FedAvg weights each client by its local example count, which
   would leak that count (a real privacy leak: sample count is job-search
   volume). This strategy ignores `fit_res.num_examples` entirely and
   averages the already-clipped-and-noised deltas each client sent
   (`app/client/privacy.py`'s output) with equal weight, then adds the
   result to the previous global parameters: `theta(r) = theta(r-1) +
   mean(delta_tilde_i)`.
2. **It refuses rounds with too few participants.** §22.4 step 3a: with
   fewer than `min_cohort` (k) participants, averaging provides no
   anonymity to hide behind, so the round is refused outright -- the
   refusal is logged, and (per the Flower `Strategy` contract) returning
   `(None, metrics)` from `aggregate_fit` means the caller keeps the
   previous global parameters active. No partial aggregation ever happens
   on a too-small cohort.
"""

from __future__ import annotations

import logging

import numpy as np
from flwr.common import EvaluateIns, EvaluateRes, FitIns, FitRes, Parameters, Scalar
from flwr.common import ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_manager import ClientManager
from flwr.server.client_proxy import ClientProxy
from flwr.server.strategy import Strategy

from app.common.parameters import flatten, unflatten

logger = logging.getLogger(__name__)

# k: the minimum cohort size at which averaging provides any anonymity at
# all (§22.4). Production would use 50+; the MVP uses 3, and the report
# says so (§22.5).
DEFAULT_MIN_COHORT = 3


class MinCohortFedAvg(Strategy):
    """Uniform-weight FedAvg over client deltas, refusing rounds below
    `min_cohort` participants.

    This is a real, direct-callable `flwr.server.strategy.Strategy`. Its
    `aggregate_fit` needs to know the previous global parameters -- Flower's
    `Strategy` interface does not pass them explicitly (a real Flower server
    tracks them itself between rounds), so callers set
    `strategy.previous_parameters` before each `aggregate_fit` call. See
    `app/server/round_runner.py` for the thin loop that does this.
    """

    def __init__(self, min_cohort: int = DEFAULT_MIN_COHORT) -> None:
        self.min_cohort = min_cohort
        self.previous_parameters: list[np.ndarray] | None = None

    # -- Strategy's abstract interface --------------------------------
    # Client selection/config and centralised evaluation are not used by
    # P43-P49's in-process, direct-call testing style (no real networked
    # client manager exists yet); real usage of these hooks is P50+ scope.

    def initialize_parameters(self, client_manager: ClientManager) -> Parameters | None:  # noqa: ARG002
        return None

    def configure_fit(
        self, server_round: int, parameters: Parameters, client_manager: ClientManager
    ) -> list[tuple[ClientProxy, FitIns]]:  # noqa: ARG002
        return []

    def configure_evaluate(
        self, server_round: int, parameters: Parameters, client_manager: ClientManager
    ) -> list[tuple[ClientProxy, EvaluateIns]]:  # noqa: ARG002
        return []

    def aggregate_evaluate(
        self,
        server_round: int,
        results: list[tuple[ClientProxy, EvaluateRes]],
        failures: list[tuple[ClientProxy, EvaluateRes] | BaseException],
    ) -> tuple[float | None, dict[str, Scalar]]:  # noqa: ARG002
        return None, {}

    def evaluate(self, server_round: int, parameters: Parameters) -> tuple[float, dict[str, Scalar]] | None:  # noqa: ARG002
        return None

    # -- the real logic -------------------------------------------------

    def aggregate_fit(
        self,
        server_round: int,
        results: list[tuple[ClientProxy, FitRes]],
        failures: list[tuple[ClientProxy, FitRes] | BaseException],  # noqa: ARG002
    ) -> tuple[Parameters | None, dict[str, Scalar]]:
        if self.previous_parameters is None:
            raise RuntimeError(
                "previous_parameters must be set before aggregate_fit is called "
                "(see app/server/round_runner.run_round)"
            )

        participant_count = len(results)
        if participant_count < self.min_cohort:
            reason = (
                f"round {server_round} refused: {participant_count} participant(s) "
                f"is below the minimum cohort size of {self.min_cohort} -- "
                "averaging over a cohort this small would not provide anonymity"
            )
            logger.warning(reason)
            return None, {
                "refused": True,
                "reason": reason,
                "participant_count": participant_count,
            }

        deltas = [flatten(parameters_to_ndarrays(fit_res.parameters)) for _, fit_res in results]

        # Uniform weighting -- deliberately NOT weighted by num_examples
        # (§22.4's rationale: that would leak each client's sample count).
        mean_delta = np.mean(deltas, axis=0)

        previous_flat = flatten(self.previous_parameters)
        new_global_flat = previous_flat + mean_delta
        new_global_parameters = unflatten(new_global_flat)

        return ndarrays_to_parameters(new_global_parameters), {
            "refused": False,
            "participant_count": participant_count,
        }
