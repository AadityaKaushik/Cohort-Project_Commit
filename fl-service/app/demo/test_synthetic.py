from __future__ import annotations

import numpy as np
import pytest

from app.common.schema import FEATURE_BOUNDS, FEATURE_COUNT, FEATURE_NAMES
from app.demo.synthetic import (
    SYNTHETIC_PREFIX,
    generate_synthetic_clients,
    is_synthetic,
    synthetic_client_id,
)

def test_same_seed_gives_the_same_clients() -> None:
    first = generate_synthetic_clients(8, seed=7)
    second = generate_synthetic_clients(8, seed=7)

    assert list(first) == list(second)
    for client_id in first:
        assert np.array_equal(first[client_id].to_array(), second[client_id].to_array())

def test_different_seeds_give_different_features() -> None:
    first = generate_synthetic_clients(3, seed=7)
    second = generate_synthetic_clients(3, seed=8)

    assert not np.array_equal(first["synthetic-client-01"].to_array(), second["synthetic-client-01"].to_array())

@pytest.mark.parametrize("seed", range(20))
def test_every_feature_stays_inside_the_schema_bounds(seed: int) -> None:
    for vector in generate_synthetic_clients(10, seed=seed).values():
        array = vector.to_array()
        assert array.shape == (FEATURE_COUNT,)
        for name, value in zip(FEATURE_NAMES, array):
            lo, hi = FEATURE_BOUNDS[name]
            assert lo <= value <= hi, name
            assert isinstance(getattr(vector, name), float)

def test_category_shares_add_up_to_one_like_real_exports() -> None:
    for vector in generate_synthetic_clients(10, seed=3).values():
        total = vector.share_dsa + vector.share_technical + vector.share_behavioural + vector.share_system_design
        assert total == pytest.approx(1.0)
        assert vector.mastery_min <= vector.mastery_mean

def test_every_generated_id_is_marked_synthetic() -> None:
    ids = list(generate_synthetic_clients(12, seed=1))

    assert ids[0] == "synthetic-client-01"
    assert ids[-1] == "synthetic-client-12"
    assert all(client_id.startswith(SYNTHETIC_PREFIX) and is_synthetic(client_id) for client_id in ids)

def test_a_real_user_id_is_not_synthetic() -> None:
    assert not is_synthetic("deadbeef-0001-4000-8000-000000000001")

def test_index_zero_is_rejected() -> None:
    with pytest.raises(ValueError):
        synthetic_client_id(0)
