from __future__ import annotations

import logging

import numpy as np
from flwr.common import EvaluateIns, EvaluateRes, FitIns, FitRes, Parameters, Scalar
from flwr.common import ndarrays_to_parameters, parameters_to_ndarrays
from flwr.server.client_manager import ClientManager
from flwr.server.client_proxy import ClientProxy
from flwr.server.strategy import Strategy

import math

from app.client.privacy import DEFAULT_CLIP_NORM, DEFAULT_NOISE_MULTIPLIER
from app.common.parameters import PARAMETER_COUNT, flatten, unflatten

logger = logging.getLogger(__name__)

DEFAULT_MIN_COHORT = 3

DEFAULT_POISONING_NORM_MULTIPLIER = 1.5

class MinCohortFedAvg(Strategy):
    def __init__(
        self,
        min_cohort: int = DEFAULT_MIN_COHORT,
        clip_norm: float = DEFAULT_CLIP_NORM,
        poisoning_norm_multiplier: float = DEFAULT_POISONING_NORM_MULTIPLIER,
        noise_multiplier: float = DEFAULT_NOISE_MULTIPLIER,
    ) -> None:
        self.min_cohort = min_cohort
        self.clip_norm = clip_norm
        self.poisoning_norm_multiplier = poisoning_norm_multiplier
        self.noise_multiplier = noise_multiplier
        self.previous_parameters: list[np.ndarray] | None = None

    @property
    def poisoning_threshold(self) -> float:
        # Updates arrive clipped to S and then noised, so an honest update has norm about
        # S * sqrt(1 + sigma^2 * d). Anything well above that was not clipped or was tampered with.
        expected_honest_norm = self.clip_norm * math.sqrt(1.0 + self.noise_multiplier**2 * PARAMETER_COUNT)
        return self.poisoning_norm_multiplier * expected_honest_norm

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
                f"is below the minimum cohort size of {self.min_cohort}. "
                "A cohort this small would not keep anyone anonymous"
            )
            logger.warning(reason)
            return None, {
                "refused": True,
                "reason": reason,
                "participant_count": participant_count,
            }

        threshold = self.poisoning_threshold
        deltas: list[np.ndarray] = []
        for client_proxy, fit_res in results:
            delta = flatten(parameters_to_ndarrays(fit_res.parameters))
            norm = float(np.linalg.norm(delta))
            if norm > threshold:
                logger.warning(
                    "round %d: dropping client %s's update: norm %.4f exceeds the poisoning "
                    "threshold %.4f. A clipped, noised update should stay well below it",
                    server_round,
                    client_proxy.cid,
                    norm,
                    threshold,
                )
                continue
            deltas.append(delta)

        if not deltas:
            reason = (
                f"round {server_round} refused: all {participant_count} participant update(s) "
                f"were filtered as anomalous (norm above {threshold:.4f}), so there is nothing to aggregate"
            )
            logger.warning(reason)
            return None, {
                "refused": True,
                "reason": reason,
                "participant_count": participant_count,
                "filtered_count": participant_count,
            }

        # Uniform weighting over the surviving deltas; num_examples would leak each client's sample count.
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
