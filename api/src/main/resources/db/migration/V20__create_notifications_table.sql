-- Rollback: db/rollback/U20__drop_notifications_table.sql

-- FR-99..101 / ARCHITECTURE_REVISION_JAVA.md §5 + §6.1 (EPIC 10): a user's
-- in-app notifications. Strictly user-owned, like every other per-user
-- table -- ON DELETE CASCADE so account deletion (US-08) leaves nothing
-- behind, and every query goes through the UserScopedRepository base.
--
-- `type` lists all five kinds §5 names up front, even though Sprint 5 only
-- produces the first three (stalled_application, upcoming_interview,
-- missing_debrief): the last two are the planned future subscribers the
-- Observer/Strategy split exists to make cheap, and widening a CHECK
-- constraint later would be a migration just to allow a string.
--
-- `channel` records which NotificationChannel strategy delivered the row;
-- only `in_app` exists today (NFR-53: email/push are drop-in strategies).
--
-- `related_entity_type`/`related_entity_id` are a loose pointer (no FK) so
-- one column pair can point at an application or an interview. They are
-- display/navigation hints only; if the target is deleted later the
-- notification simply links to a "not found" page, same as a stale bookmark.
--
-- `dedupe_key` is what makes the scheduled detector idempotent: it names
-- the specific occurrence being reported (e.g. one application's stay in
-- one stage, one interview at one scheduled time), and the partial unique
-- index below lets the insert be ON CONFLICT DO NOTHING -- so the same
-- stall is never notified twice no matter how often detection runs.
CREATE TABLE notifications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type                 TEXT NOT NULL
                             CHECK (type IN ('stalled_application', 'upcoming_interview', 'missing_debrief',
                                              'new_recommendation', 'fl_round_result')),
    title                TEXT NOT NULL,
    body                 TEXT NOT NULL,
    related_entity_type  TEXT CHECK (related_entity_type IN ('application', 'interview')),
    related_entity_id    UUID,
    channel              TEXT NOT NULL DEFAULT 'in_app' CHECK (channel IN ('in_app')),
    dedupe_key           TEXT,
    is_read              BOOLEAN NOT NULL DEFAULT false,
    read_at              TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notifications_related_entity
        CHECK ((related_entity_type IS NULL) = (related_entity_id IS NULL)),
    CONSTRAINT chk_notifications_read_at
        CHECK (is_read OR read_at IS NULL)
);

-- §5: the notification bell's "unread first, newest first" query.
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, is_read, created_at DESC);

CREATE UNIQUE INDEX uq_notifications_user_dedupe_key ON notifications (user_id, dedupe_key)
    WHERE dedupe_key IS NOT NULL;
