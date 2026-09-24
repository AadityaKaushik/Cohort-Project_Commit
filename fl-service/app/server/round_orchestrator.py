"""Drives one privacy-ledger round over whatever clients have currently
exported features (P55, US-63).

Deliberately simpler than `app/server/round_runner.py` (P47): this module
does not run local SGD training or FedAvg aggregation -- P43-P49 already
built and tested that pipeline end to end against synthetic client deltas
(`test_strategy.py`, `test_model_registry.py`). What P55 needs is the
Privacy Ledger's core honesty property -- "the exact transmitted vector,"
not a placeholder -- so this module's job is narrower and more literal:
take each opted-in client's already-exported feature vector
(`app/control/client_store.py`, P51) and run it through the REAL,
already-tested clip-then-noise pipeline (`app/client/privacy.py`, P46),
unmodified. The vector that comes out of `add_gaussian_noise` here is
exactly what gets recorded -- there is no separate/parallel code path that
could drift from what P46 actually computes.

A round still enforces the same minimum-cohort rule as `app/server/strategy.py`
(US-61) for its own `accepted`/`reason` metadata -- but unlike
`MinCohortFedAvg.aggregate_fit`, refusal here does not withhold the ledger
entries: US-63 is "a log of everything my client has ever sent," so a
client's contribution is still recorded even in a round that, as a whole,
had too few participants to aggregate.
"""

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
    """`features_by_client`: `client_id -> (schema_version, raw_vector)`,
    straight from `ClientFeatureStore.all()`. Every entry's
    `transmitted_vector` is the actual, real output of
    `app.client.privacy.add_gaussian_noise` -- never recomputed or
    approximated afterwards.
    """
    cohort_size = len(features_by_client)
    entries: list[LedgerEntry] = []
    for client_id, (schema_version, raw_vector) in features_by_client.items():
        pre_clip_norm = float(np.linalg.norm(raw_vector))
        clipped = clip_update(raw_vector, clip_norm=clip_norm)
        post_clip_norm = float(np.linalg.norm(clipped))
        noised = add_gaussian_noise(clipped, rng, sigma=noise_multiplier, clip_norm=clip_norm)
        # A simple, commonly-cited Gaussian-mechanism epsilon approximation
        # (sensitivity / sigma) for the Privacy Ledger's "epsilon consumed"
        # column -- illustrative, not a rigorous privacy-accountant
        # computation (composition across rounds is out of scope here).
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
            f"minimum cohort size of {min_cohort} -- averaging over a cohort this small "
            "would not provide anonymity"
        )
    )
    return RoundOutcome(round_number=round_number, accepted=accepted, reason=reason, cohort_size=cohort_size, entries=entries)
