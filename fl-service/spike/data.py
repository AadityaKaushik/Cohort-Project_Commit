"""Synthetic, non-IID two-client data for the P32 FedAvg spike.

Deliberately synthetic (sklearn.datasets.make_classification) -- this spike's
job is to de-risk the FedAvg *mechanics*, not to reproduce Cohort's real
feature schema (that's Sprint 4's US-58, `app/common/schema.py`). The two
client shards are drawn with different `flip_y`/class-balance settings so
they are non-IID, which is the realistic case federated averaging has to
handle -- an IID split would make every risk this spike is meant to surface
invisible.
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from sklearn.datasets import make_classification
from sklearn.model_selection import train_test_split

RANDOM_SEED = 42
N_FEATURES = 8


@dataclass(frozen=True)
class ClientShard:
    x_train: np.ndarray
    y_train: np.ndarray


@dataclass(frozen=True)
class SpikeDataset:
    client_shards: list[ClientShard]
    x_test: np.ndarray
    y_test: np.ndarray


def build_two_client_dataset() -> SpikeDataset:
    """Two non-IID client shards plus one shared held-out test set."""
    x, y = make_classification(
        n_samples=600,
        n_features=N_FEATURES,
        n_informative=5,
        n_redundant=1,
        class_sep=1.2,
        random_state=RANDOM_SEED,
    )
    x_train_full, x_test, y_train_full, y_test = train_test_split(
        x, y, test_size=0.25, random_state=RANDOM_SEED, stratify=y
    )

    # Non-IID split: client 0 gets a class-skewed slice, client 1 gets the rest.
    # This is the shape real Cohort users would have too -- one heavy user, one
    # light one, neither with the same label balance as the population.
    rng = np.random.default_rng(RANDOM_SEED)
    order = rng.permutation(len(x_train_full))
    x_shuffled, y_shuffled = x_train_full[order], y_train_full[order]

    positive_idx = np.where(y_shuffled == 1)[0]
    negative_idx = np.where(y_shuffled == 0)[0]

    # Client 0: skewed toward the positive class (70% of positives, 30% of negatives).
    client0_idx = np.concatenate(
        [positive_idx[: int(0.7 * len(positive_idx))], negative_idx[: int(0.3 * len(negative_idx))]]
    )
    client1_idx = np.array([i for i in range(len(x_shuffled)) if i not in set(client0_idx.tolist())])

    shards = [
        ClientShard(x_shuffled[client0_idx], y_shuffled[client0_idx]),
        ClientShard(x_shuffled[client1_idx], y_shuffled[client1_idx]),
    ]
    return SpikeDataset(client_shards=shards, x_test=x_test, y_test=y_test)
