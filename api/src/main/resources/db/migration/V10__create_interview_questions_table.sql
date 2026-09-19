-- Rollback: db/rollback/U10__drop_interview_questions_table.sql

-- topic_id and linked_question_id reference tables (topics, questions) that
-- don't exist until the Sprint 3 prep-bank migration (P27/P28); both
-- columns exist now, nullable and without a FK constraint, exactly the
-- deferred-FK pattern V4/V7 already used for applications.resume_id. Their
-- FK constraints are added by an ALTER TABLE in that later migration.
CREATE TABLE interview_questions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id        UUID NOT NULL REFERENCES interviews (id) ON DELETE CASCADE,
    -- Denormalised for the same isolation reason as interviews.user_id.
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- DR-10: question text is frequently NDA-constrained — private by
    -- default, never surfaced anywhere shared, never included in any FL
    -- payload (only topic-level aggregates ever leave this table).
    question_text       TEXT NOT NULL,
    topic_id            UUID,
    category            TEXT NOT NULL
                            CHECK (category IN ('dsa', 'technical', 'behavioural', 'hr',
                                                 'system_design', 'company_specific')),
    difficulty          TEXT CHECK (difficulty IN ('easy', 'medium', 'hard')),
    self_rating         SMALLINT CHECK (self_rating BETWEEN 1 AND 5),
    time_taken_minutes  INTEGER,
    linked_question_id  UUID,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interview_questions_interview_id ON interview_questions (interview_id, created_at);
CREATE INDEX idx_interview_questions_user_id ON interview_questions (user_id);
CREATE INDEX idx_interview_questions_topic_id ON interview_questions (topic_id);
