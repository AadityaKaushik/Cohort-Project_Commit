from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from app.client.local_trainer import MIN_LOCAL_EXAMPLES, train_local_round
from app.client.privacy import DEFAULT_CLIP_NORM, DEFAULT_NOISE_MULTIPLIER, add_gaussian_noise, clip_update
from app.common.parameters import flatten
from app.control.client_store import ClientFeatures
from app.server.evaluation import check_set_accuracy
from app.server.model_registry import ModelRegistry
from app.server.round_runner import run_round
from app.server.strategy import DEFAULT_MIN_COHORT, MinCohortFedAvg


@dataclass(frozen=True)
class LedgerEntry:
    client_id: str
    schema_version: int
    pre_clip_norm: float
    post_clip_norm: float
    noise_multiplier: float
    epsilon: float
    transmitted_vector: list[float]


@dataclass(frozen=True)
class RoundOutcome:
    round_number: int
    accepted: bool
    reason: str | None
    cohort_size: int  # clients that trained and sent an update
    entries: list[LedgerEntry]
    sat_out_count: int = 0  # clients with too few labelled rounds to train this time
    filtered_count: int = 0
    model_version: int = 0  # the version serving after this round
    check_accuracy: float | None = None


def run_training_round(
    clients: dict[str, ClientFeatures],
    round_number: int,
    registry: ModelRegistry,
    rng: np.random.Generator,
    min_cohort: int = DEFAULT_MIN_COHORT,
    clip_norm: float = DEFAULT_CLIP_NORM,
    noise_multiplier: float = DEFAULT_NOISE_MULTIPLIER,
) -> RoundOutcome:
    """
    One federated round. Each client trains the current global model on its own labelled rounds, then only
    the change it made leaves it: clipped to S and noised. The server averages the noised changes; it never
    sees a client's examples. A refused round leaves the current version serving.
    """
    global_parameters = registry.current.parameters
    global_flat = flatten(global_parameters)
    entries: list[LedgerEntry] = []
    updates: list[tuple[str, np.ndarray, int]] = []
    sat_out = 0

    for index, (client_id, record) in enumerate(sorted(clients.items())):
        if record.example_count < MIN_LOCAL_EXAMPLES:
            sat_out += 1
            continue
        trained = train_local_round(global_parameters, record.examples_x, record.examples_y, seed=round_number * 1000 + index)
        delta = flatten(trained) - global_flat
        clipped = clip_update(delta, clip_norm=clip_norm)
        noised = add_gaussian_noise(clipped, rng, sigma=noise_multiplier, clip_norm=clip_norm)
        updates.append((client_id, noised, record.example_count))
        entries.append(
            LedgerEntry(
                client_id=client_id,
                schema_version=record.schema_version,
                pre_clip_norm=float(np.linalg.norm(delta)),
                post_clip_norm=float(np.linalg.norm(clipped)),
                noise_multiplier=noise_multiplier,
                epsilon=clip_norm / noise_multiplier if noise_multiplier > 0 else float("inf"),
                transmitted_vector=noised.tolist(),
            )
        )

    strategy = MinCohortFedAvg(min_cohort=min_cohort, clip_norm=clip_norm, noise_multiplier=noise_multiplier)
    result = run_round(strategy, round_number, global_parameters, updates)
    check_accuracy = None
    if result.accepted:
        check_accuracy = check_set_accuracy(result.parameters)
        registry.publish(result.parameters, round_number=round_number, participant_count=result.participant_count, check_accuracy=check_accuracy)
    reason = result.reason
    if not result.accepted and not updates:
        reason = (
            f"round {round_number} refused: no client had at least {MIN_LOCAL_EXAMPLES} rated interview rounds to "
            f"train on, which is below the minimum cohort size of {min_cohort}"
        )
    return RoundOutcome(
        round_number=round_number,
        accepted=result.accepted,
        reason=reason,
        cohort_size=len(updates),
        entries=entries,
        sat_out_count=sat_out,
        filtered_count=result.filtered_count,
        model_version=registry.current.version,
        check_accuracy=check_accuracy if result.accepted else registry.current.check_accuracy,
    )
