-- Rollback: db/rollback/U18__drop_recommendation_evidence_table.sql

-- US-51/US-53: "each cites the observations behind it" -- one row per
-- concrete observation a recommendation is built from (a low-confidence
-- practice attempt, a dwell-time fact, a prerequisite's mastery score).
-- Deliberately generic (label/detail/occurred_at) rather than one column
-- per generator's evidence shape, since every generator's evidence is
-- fundamentally "here is a fact and when it happened" -- this is reference
-- data owned indirectly through recommendation_id, not directly by a
-- user_id column, so it does not extend UserScopedRepository (same
-- rationale as TopicRepository's own comment).
CREATE TABLE recommendation_evidence (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recommendation_id  UUID NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    label              TEXT NOT NULL,
    detail             TEXT,
    occurred_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendation_evidence_recommendation_id ON recommendation_evidence (recommendation_id);
