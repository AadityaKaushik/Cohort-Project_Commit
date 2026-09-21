"""Feature schema v1 -- the single source of truth for what a Cohort FL
client is allowed to compute and transmit (US-58, PROJECT_PLAN.md §22.2).

Fixed length: exactly 24 `float32` features, each bounded to [0,1] or
[-1,1], each derived locally from one client's own data, each bucketed or
log-scaled so no raw count is recoverable from a value alone (a raw count of
137 applications is close to a fingerprint; `log1p(137)/log1p(300)` is not).
Published verbatim at `GET /fl/schema` (Sprint 4, P54+) and shown on the
opt-in screen.

This module is deliberately the *only* place the 24 names, their order, and
their bounds are declared. Everything downstream -- the extractor
(`app/client/feature_extractor.py`), the local trainer
(`app/client/local_trainer.py`), and the server's expected parameter shape
(`app/server/`) -- consumes `FEATURE_NAMES` / `FeatureVector` rather than
re-typing the list, which is exactly the kind of drift the schema test
below exists to catch.

What is provably absent from this schema, by construction: no strings, no
user or record identifiers, no company names, no job titles, no dates or
timestamps, no free text, no interviewer names, no resume content, no
question text, no salary, no location. Every field is a bounded float.
"""

from __future__ import annotations

from dataclasses import dataclass, fields

import numpy as np

# name -> (lower_bound, upper_bound), in PROJECT_PLAN.md §22.2 order. This
# dict's key order IS the schema order -- `FEATURE_NAMES` is derived from it
# rather than declared a second time.
FEATURE_BOUNDS: dict[str, tuple[float, float]] = {
    "mastery_mean": (0.0, 1.0),
    "mastery_min": (0.0, 1.0),
    "mastery_std": (0.0, 1.0),
    "practice_volume": (0.0, 1.0),
    "topic_breadth": (0.0, 1.0),
    "practice_recency": (0.0, 1.0),
    "practice_consistency": (0.0, 1.0),
    "confidence_mean": (0.0, 1.0),
    "confidence_trend": (-1.0, 1.0),
    "hard_ratio": (0.0, 1.0),
    "share_dsa": (0.0, 1.0),
    "share_technical": (0.0, 1.0),
    "share_behavioural": (0.0, 1.0),
    "share_system_design": (0.0, 1.0),
    "task_completion_rate": (0.0, 1.0),
    "prep_lead_time": (0.0, 1.0),
    "interview_experience": (0.0, 1.0),
    "rounds_per_application": (0.0, 1.0),
    "recent_self_rating": (0.0, 1.0),
    "coverage_ratio": (0.0, 1.0),
    "repeat_topic_ratio": (0.0, 1.0),
    "application_volume": (0.0, 1.0),
    "resume_variants": (0.0, 1.0),
    "round_type_ordinal": (0.0, 1.0),
}

FEATURE_NAMES: tuple[str, ...] = tuple(FEATURE_BOUNDS.keys())
FEATURE_COUNT = 24

_BOUND_EPSILON = 1e-6


@dataclass(frozen=True)
class FeatureVector:
    """Exactly the 24 named, bounded floats of schema v1, in schema order.

    Field order below is not cosmetic: `to_array()` and every downstream
    consumer (the local trainer's `X` matrix columns, the server's expected
    parameter shape) rely on this exact ordering matching `FEATURE_NAMES`.
    """

    mastery_mean: float
    mastery_min: float
    mastery_std: float
    practice_volume: float
    topic_breadth: float
    practice_recency: float
    practice_consistency: float
    confidence_mean: float
    confidence_trend: float
    hard_ratio: float
    share_dsa: float
    share_technical: float
    share_behavioural: float
    share_system_design: float
    task_completion_rate: float
    prep_lead_time: float
    interview_experience: float
    rounds_per_application: float
    recent_self_rating: float
    coverage_ratio: float
    repeat_topic_ratio: float
    application_volume: float
    resume_variants: float
    round_type_ordinal: float

    def __post_init__(self) -> None:
        for name in FEATURE_NAMES:
            value = getattr(self, name)
            if isinstance(value, bool) or not isinstance(value, (int, float)):
                raise TypeError(f"{name} must be a float, got {type(value).__name__}")
            lo, hi = FEATURE_BOUNDS[name]
            if not (lo - _BOUND_EPSILON <= float(value) <= hi + _BOUND_EPSILON):
                raise ValueError(f"{name}={value!r} out of bounds [{lo}, {hi}]")

    def to_array(self) -> np.ndarray:
        """The exact 24-float payload that leaves the client, in schema order."""
        return np.array([getattr(self, name) for name in FEATURE_NAMES], dtype=np.float32)

    @staticmethod
    def field_names() -> tuple[str, ...]:
        return tuple(f.name for f in fields(FeatureVector))
