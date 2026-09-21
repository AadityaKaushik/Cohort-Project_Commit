"""Model versioning (US-62, P49): an append-only history of global models.

The safety property that matters here: **a failed or refused round must
never overwrite a good model.** §22.4 step 3a/3b already guarantee a
refused or fully-filtered round returns no new parameters
(`app/server/strategy.py`, `app/server/round_runner.py`); this module is
what makes that guarantee durable and inspectable -- publishing is the only
way a version is ever added, nothing is ever mutated or removed, and
`current()` always answers from the latest *published* version, which by
construction is always the latest *accepted* round's model, never a
refused one's.
"""

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
    """Append-only. `publish` is the only mutating operation, and it only
    ever appends -- there is no update-in-place or delete path, the same
    append-only discipline `PracticeAttempt` uses on the Java side and for
    the same reason: a wrong or malicious write can be superseded by a new
    version, but never allowed to silently replace history.
    """

    def __init__(self, initial_parameters: list[np.ndarray]) -> None:
        self._versions: list[ModelVersion] = [
            ModelVersion(version=0, parameters=initial_parameters, round_number=0, participant_count=0)
        ]

    @property
    def current(self) -> ModelVersion:
        """The latest published version -- always servable, even if the
        most recent round attempted was refused."""
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
    """Runs one FL round against the registry's current model and, only on
    acceptance, publishes the result as a new version. A refused round (too
    few participants, or every update filtered as anomalous) publishes
    nothing: `registry.current` still answers with the previous version
    afterwards, exactly as if the round had never been attempted.
    """
    result = run_round(strategy, server_round, registry.current.parameters, client_updates)
    if result.accepted:
        registry.publish(result.parameters, round_number=server_round, participant_count=result.participant_count)
    return result
