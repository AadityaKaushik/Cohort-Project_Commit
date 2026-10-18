from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone

import numpy as np

from app.server.round_runner import RoundResult, run_round
from app.server.strategy import MinCohortFedAvg

@dataclass(frozen=True)
class ModelVersion:
    version: int
    parameters: list[np.ndarray]
    round_number: int
    participant_count: int
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    check_accuracy: float | None = None  # accuracy on the synthetic check set, demo only

class ModelRegistry:
    def __init__(self, initial_parameters: list[np.ndarray], initial_check_accuracy: float | None = None) -> None:
        self._initial = ModelVersion(
            version=0, parameters=initial_parameters, round_number=0, participant_count=0, check_accuracy=initial_check_accuracy
        )
        self._versions: list[ModelVersion] = [self._initial]

    def reset(self) -> None:
        """Back to version 0. Only the demo reset uses this."""
        self._versions = [self._initial]

    @property
    def current(self) -> ModelVersion:
        """The latest published version, even if the last round was refused."""
        return self._versions[-1]

    @property
    def history(self) -> tuple[ModelVersion, ...]:
        return tuple(self._versions)

    def get(self, version: int) -> ModelVersion:
        for model_version in self._versions:
            if model_version.version == version:
                return model_version
        raise KeyError(f"no such model version: {version}")

    def publish(
        self,
        parameters: list[np.ndarray],
        round_number: int,
        participant_count: int,
        check_accuracy: float | None = None,
    ) -> ModelVersion:
        new_version = ModelVersion(
            version=self.current.version + 1,
            parameters=parameters,
            round_number=round_number,
            participant_count=participant_count,
            check_accuracy=check_accuracy,
        )
        self._versions.append(new_version)
        return new_version

def run_and_publish_round(
    registry: ModelRegistry,
    strategy: MinCohortFedAvg,
    server_round: int,
    client_updates: list[tuple[str, np.ndarray, int]],
) -> RoundResult:
    result = run_round(strategy, server_round, registry.current.parameters, client_updates)
    if result.accepted:
        registry.publish(result.parameters, round_number=server_round, participant_count=result.participant_count)
    return result
