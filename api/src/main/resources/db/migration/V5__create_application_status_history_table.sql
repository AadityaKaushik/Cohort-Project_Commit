-- Rollback: db/rollback/U5__drop_application_status_history_table.sql

-- Append-only by design (PROJECT_PLAN.md §17.2): no update endpoint exists
-- for this table and it has no updated_at column. It is the source of
-- truth for the funnel (§20.7) — current status alone cannot tell you
-- whether an application ever reached the interview stage before being
-- rejected (FR-20, DR-01).
CREATE TABLE application_status_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    from_status    TEXT,
    to_status      TEXT NOT NULL,
    note           TEXT,
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_status_history_app_changed_at
    ON application_status_history (application_id, changed_at DESC);
