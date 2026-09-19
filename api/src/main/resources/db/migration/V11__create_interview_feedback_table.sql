-- Rollback: db/rollback/U11__drop_interview_feedback_table.sql

-- One-to-one with interviews (UNIQUE on interview_id). self_rating is
-- nullable, not NOT NULL, because US-30 requires partial completion to save
-- as a draft (is_draft = true) before every field is known.
CREATE TABLE interview_feedback (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id       UUID NOT NULL UNIQUE REFERENCES interviews (id) ON DELETE CASCADE,
    -- Denormalised for the same isolation reason as interviews.user_id.
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    self_rating        SMALLINT CHECK (self_rating BETWEEN 1 AND 5),
    what_went_well     TEXT,
    what_went_poorly   TEXT,
    improvement_areas  TEXT,
    -- US-32 (Could, Sprint 5): recruiter/interviewer feedback relayed back
    -- to the candidate. The column exists now so that later story doesn't
    -- need its own migration; unused until then.
    external_feedback  TEXT,
    is_draft           BOOLEAN NOT NULL DEFAULT true,
    submitted_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_feedback_user_id ON interview_feedback (user_id);
