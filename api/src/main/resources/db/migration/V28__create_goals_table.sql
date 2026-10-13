-- Rollback: db/rollback/U28__drop_goals_table.sql

-- US-101: a user's weekly activity targets. One target per metric per user;
-- progress is derived from the user's own activity, never stored.
CREATE TABLE goals (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    metric        TEXT NOT NULL
                      CHECK (metric IN ('applications', 'interviews', 'practice_attempts', 'contacts_reached')),
    weekly_target INTEGER NOT NULL CHECK (weekly_target > 0 AND weekly_target <= 1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_goals_user_metric UNIQUE (user_id, metric)
);
