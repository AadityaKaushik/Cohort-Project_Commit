-- Rollback: db/rollback/U102__drop_stories_table.sql

-- Reusable STAR stories (situation, task, action, result) tagged by competency,
-- for behavioural interview rounds. Strictly user-owned. The interview link is
-- optional and SET NULL, so deleting an interview keeps the story.
CREATE TABLE stories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    situation    TEXT,
    task         TEXT,
    action       TEXT,
    result       TEXT,
    competencies TEXT[] NOT NULL DEFAULT '{}'
                     CHECK (competencies <@ ARRAY['leadership', 'teamwork', 'conflict', 'failure', 'ownership',
                                                  'deadline', 'learning', 'impact', 'communication', 'problem_solving']),
    interview_id UUID REFERENCES interviews (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stories_user_id ON stories (user_id);
