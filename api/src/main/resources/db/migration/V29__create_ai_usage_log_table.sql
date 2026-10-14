-- Rollback: db/rollback/U29__drop_ai_usage_log_table.sql

-- US-102: one row per AI call, for rate limiting review and cost checks.
-- Prompt and reply text are never stored, only sizes and timing.
CREATE TABLE ai_usage_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    feature        TEXT NOT NULL,
    provider       TEXT NOT NULL,
    succeeded      BOOLEAN NOT NULL,
    prompt_chars   INTEGER NOT NULL,
    response_chars INTEGER NOT NULL,
    duration_ms    INTEGER NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_log_user_created ON ai_usage_log (user_id, created_at);
