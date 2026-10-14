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

class ModelRegistry:
    def __init__(self, initial_parameters: list[np.ndarray]) -> None:
        self._versions: list[ModelVersion] = [
            ModelVersion(version=0, parameters=initial_parameters, round_number=0, participant_count=0)
        ]

    @property
    def current(self) -> ModelVersion:
        """The latest published version -- always servable, even if the most recent round attempted was refused."""
        return self._versions[-1]

    @property
    def history(self) -> tuple[ModelVersion, ...]:
        return tuple(self._versions)

    def get(self, version: int) -> ModelVersion:
        for model_version in self._versions:
            if model_version.version == version:
                return model_version
        raise KeyError(f"no such model version: {version}")

    def publish(self, parameters: list[np.ndarray], round_number: int, participant_count: int) -> ModelVersion:
        new_version = ModelVersion(
            version=self.current.version + 1,
            parameters=parameters,
            round_number=round_number,
            participant_count=participant_count,
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
