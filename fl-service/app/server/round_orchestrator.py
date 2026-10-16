from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from app.client.privacy import DEFAULT_CLIP_NORM, DEFAULT_NOISE_MULTIPLIER, add_gaussian_noise, clip_update
from app.server.strategy import DEFAULT_MIN_COHORT

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
    cohort_size: int
    entries: list[LedgerEntry]

def run_privacy_round(
    features_by_client: dict[str, tuple[int, np.ndarray]],
    round_number: int,
    rng: np.random.Generator,
    min_cohort: int = DEFAULT_MIN_COHORT,
    clip_norm: float = DEFAULT_CLIP_NORM,
    noise_multiplier: float = DEFAULT_NOISE_MULTIPLIER,
) -> RoundOutcome:
    cohort_size = len(features_by_client)
    entries: list[LedgerEntry] = []
    for client_id, (schema_version, raw_vector) in features_by_client.items():
        pre_clip_norm = float(np.linalg.norm(raw_vector))
        clipped = clip_update(raw_vector, clip_norm=clip_norm)
        post_clip_norm = float(np.linalg.norm(clipped))
        noised = add_gaussian_noise(clipped, rng, sigma=noise_multiplier, clip_norm=clip_norm)
        epsilon = clip_norm / noise_multiplier if noise_multiplier > 0 else float("inf")
        entries.append(
            LedgerEntry(
                client_id=client_id,
                schema_version=schema_version,
                pre_clip_norm=pre_clip_norm,
                post_clip_norm=post_clip_norm,
                noise_multiplier=noise_multiplier,
                epsilon=epsilon,
                transmitted_vector=noised.tolist(),
            )
        )

    accepted = cohort_size >= min_cohort
    reason = (
        None
        if accepted
        else (
            f"round {round_number} refused: {cohort_size} participant(s) is below the "
            f"minimum cohort size of {min_cohort}. A cohort this small "
            "would not keep anyone anonymous"
        )
    )
    return RoundOutcome(round_number=round_number, accepted=accepted, reason=reason, cohort_size=cohort_size, entries=entries)
