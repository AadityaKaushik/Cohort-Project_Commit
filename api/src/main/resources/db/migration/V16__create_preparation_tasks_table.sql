-- Rollback: db/rollback/U16__drop_preparation_tasks_table.sql

-- US-40: a user's own preparation task list, linked to a topic so
-- completing tasks feeds the progress view (US-41) alongside mastery
-- scores. Strictly user-owned, like practice_attempts.
CREATE TABLE preparation_tasks (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id                  UUID NOT NULL REFERENCES topics (id),
    title                     TEXT NOT NULL,
    description               TEXT,
    due_date                  DATE,
    estimated_effort_minutes  INTEGER,
    status                    TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'completed')),
    completed_at              TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_preparation_tasks_user_status ON preparation_tasks (user_id, status);
CREATE INDEX idx_preparation_tasks_user_due_date ON preparation_tasks (user_id, due_date);
CREATE INDEX idx_preparation_tasks_topic_id ON preparation_tasks (topic_id);
