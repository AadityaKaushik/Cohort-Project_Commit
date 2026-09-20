-- Rollback: db/rollback/U15__drop_practice_attempts_table.sql

-- US-37: append-only practice log (PROJECT_PLAN.md §17.2, line 1545) — a
-- repeated attempt at the same question inserts a new row rather than
-- overwriting the previous one, so a confidence trajectory over time
-- exists for the mastery engine (P30) to apply time decay against.
CREATE TABLE practice_attempts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    status              TEXT NOT NULL
                            CHECK (status IN ('not_started', 'in_progress', 'confident', 'needs_review')),
    confidence          SMALLINT CHECK (confidence BETWEEN 1 AND 5),
    time_taken_minutes  INTEGER,
    notes               TEXT,
    attempted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_practice_attempts_user_question_attempted
    ON practice_attempts (user_id, question_id, attempted_at DESC);
