from __future__ import annotations

import numpy as np
import pytest

from app.common.schema import FEATURE_BOUNDS, FEATURE_COUNT, FEATURE_NAMES, FeatureVector

_MIDPOINTS = {name: (lo + hi) / 2 for name, (lo, hi) in FEATURE_BOUNDS.items()}

def make_vector(**overrides: float) -> FeatureVector:
    values = dict(_MIDPOINTS)
    values.update(overrides)
    return FeatureVector(**values)

def test_schema_declares_exactly_24_features() -> None:
    assert FEATURE_COUNT == 24
    assert len(FEATURE_NAMES) == 24
    assert len(FEATURE_BOUNDS) == 24

def test_feature_vector_has_exactly_the_24_schema_fields_in_order() -> None:
    assert FeatureVector.field_names() == FEATURE_NAMES

def test_feature_names_are_unique() -> None:
    assert len(set(FEATURE_NAMES)) == 24

def test_schema_matches_project_plan_section_22_2_order() -> None:
    assert FEATURE_NAMES[0] == "mastery_mean"
    assert FEATURE_NAMES[23] == "round_type_ordinal"
    assert FEATURE_NAMES[8] == "confidence_trend"  # the sole [-1,1] feature

def test_to_array_serialises_to_24_named_floats_in_schema_order() -> None:
    vector = make_vector()
    array = vector.to_array()

    assert array.shape == (24,)
    assert array.dtype == np.float32
    assert list(array) == [pytest.approx(getattr(vector, name)) for name in FEATURE_NAMES]

def test_confidence_trend_allows_negative_values() -> None:
    make_vector(confidence_trend=-1.0)  # must not raise

@pytest.mark.parametrize("name", ["mastery_mean", "practice_volume", "round_type_ordinal"])
def test_zero_to_one_features_reject_negative_values(name: str) -> None:
    with pytest.raises(ValueError):
        make_vector(**{name: -0.01})

def test_out_of_bounds_above_one_is_rejected() -> None:
    with pytest.raises(ValueError):
        make_vector(mastery_mean=1.5)

def test_non_numeric_value_is_rejected() -> None:
    with pytest.raises(TypeError):
        make_vector(mastery_mean="high")  # type: ignore[arg-type]

def test_adding_a_25th_feature_is_not_representable() -> None:
    with pytest.raises(TypeError):
        make_vector(extra_feature=0.5)  # type: ignore[call-arg]
