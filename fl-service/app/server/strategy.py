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
3. **It filters anomalously large updates before aggregating (US-84, P48,
   §22.4 step 3b).** After clipping (`app/client/privacy.py`), an honest
   client's transmitted delta can never exceed norm `S`; a delta with norm
   greater than `S * poisoning_norm_multiplier` (tau, default 3) is
   evidence of a malicious or malfunctioning client bypassing its own
   clipping step, and is dropped -- logged, never averaged in -- before the
   uniform mean is computed. The min-cohort check still runs against the
   *original* participant count (§22.4's ordering: 3a before 3b), but if
   filtering leaves zero survivors the round is refused too, since there is
   nothing left to aggregate.
"""

from __future__ import annotations

import logging

import numpy as np
from flwr.common import EvaluateIns, EvaluateRes, FitIns, FitRes, Parameters, Scalar
from flwr.common import ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_manager import ClientManager
from flwr.server.client_proxy import ClientProxy
from flwr.server.strategy import Strategy

from app.client.privacy import DEFAULT_CLIP_NORM
from app.common.parameters import flatten, unflatten

logger = logging.getLogger(__name__)

# k: the minimum cohort size at which averaging provides any anonymity at
# all (§22.4). Production would use 50+; the MVP uses 3, and the report
# says so (§22.5).
DEFAULT_MIN_COHORT = 3

# tau: rejects updates more than 3x the clipping norm S, which after
# honest clipping should be impossible to exceed (§22.4).
DEFAULT_POISONING_NORM_MULTIPLIER = 3.0


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

    def __init__(
        self,
        min_cohort: int = DEFAULT_MIN_COHORT,
        clip_norm: float = DEFAULT_CLIP_NORM,
        poisoning_norm_multiplier: float = DEFAULT_POISONING_NORM_MULTIPLIER,
    ) -> None:
        self.min_cohort = min_cohort
        self.clip_norm = clip_norm
        self.poisoning_norm_multiplier = poisoning_norm_multiplier
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

        threshold = self.clip_norm * self.poisoning_norm_multiplier
        deltas: list[np.ndarray] = []
        for client_proxy, fit_res in results:
            delta = flatten(parameters_to_ndarrays(fit_res.parameters))
            norm = float(np.linalg.norm(delta))
            if norm > threshold:
                logger.warning(
                    "round %d: dropping client %s's update: norm %.4f exceeds poisoning "
                    "threshold %.4f (S=%.4f * tau=%.4f) -- an honestly clipped update "
                    "should never exceed S",
                    server_round,
                    client_proxy.cid,
                    norm,
                    threshold,
                    self.clip_norm,
                    self.poisoning_norm_multiplier,
                )
                continue
            deltas.append(delta)

        if not deltas:
            reason = (
                f"round {server_round} refused: all {participant_count} participant update(s) "
                f"were filtered as anomalous (norm above {threshold:.4f}) -- nothing left to aggregate"
            )
            logger.warning(reason)
            return None, {
                "refused": True,
                "reason": reason,
                "participant_count": participant_count,
                "filtered_count": participant_count,
            }

        # Uniform weighting over the *surviving* deltas -- deliberately NOT
        # weighted by num_examples (§22.4's rationale: that would leak each
        # client's sample count).
        mean_delta = np.mean(deltas, axis=0)

        previous_flat = flatten(self.previous_parameters)
        new_global_flat = previous_flat + mean_delta
        new_global_parameters = unflatten(new_global_flat)

        return ndarrays_to_parameters(new_global_parameters), {
            "refused": False,
            "participant_count": participant_count,
            "aggregated_count": len(deltas),
            "filtered_count": participant_count - len(deltas),
        }
