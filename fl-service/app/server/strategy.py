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

DEFAULT_MIN_COHORT = 3

DEFAULT_POISONING_NORM_MULTIPLIER = 3.0

class MinCohortFedAvg(Strategy):
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

        # Uniform weighting over the *surviving* deltas -- deliberately NOT weighted by num_examples (§22.4's rationale: that would leak each client's sample count).
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
