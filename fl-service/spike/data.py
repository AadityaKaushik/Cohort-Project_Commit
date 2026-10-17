"""Synthetic, non-IID data for two clients, for the FedAvg experiment.

The shards differ in class balance and label noise, since an IID split would hide
the problems the experiment is meant to show.
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

    # Non-IID split: client 0 gets a class-skewed slice, client 1 the rest.
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
