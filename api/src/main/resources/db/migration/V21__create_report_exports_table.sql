-- Rollback: db/rollback/U21__drop_report_exports_table.sql

-- FR-102..104 / ARCHITECTURE_REVISION_JAVA.md §5 + §6.2 (US-80): one row per
-- generated PDF/CSV export. Strictly user-owned (ON DELETE CASCADE, and every
-- query goes through UserScopedRepository). The file itself lives on disk
-- under the report storage root -- outside the web root, never publicly
-- served -- and `file_path` is the server-generated storage key, never
-- anything derived from user input: the exact storage rules resumes follow
-- (US-21/22, FR-32).
--
-- `report_type` adds `progress_report` (applications + analytics summary in
-- one PDF, the "share my progress with a mentor" case US-80 describes) to
-- §5's two types; the CHECK below keeps it PDF-only, since a CSV has no
-- sensible way to hold two unrelated tables.
--
-- `expires_at` is what makes the download "expiring" at the resource level
-- (FR-104), on top of the short-lived bearer JWT every download already
-- requires: after it passes the download is refused and a cleanup job
-- removes both the row and the file. `file_name`/`content_type`/
-- `file_size_bytes` are what the download response and the exports list
-- need, captured once at generation time.
CREATE TABLE report_exports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    report_type      TEXT NOT NULL
                         CHECK (report_type IN ('applications', 'analytics_summary', 'progress_report')),
    format           TEXT NOT NULL CHECK (format IN ('pdf', 'csv')),
    date_range_from  DATE,
    date_range_to    DATE,
    file_path        TEXT NOT NULL,
    file_name        TEXT NOT NULL,
    content_type     TEXT NOT NULL,
    file_size_bytes  BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    generated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_report_exports_date_range
        CHECK (date_range_from IS NULL OR date_range_to IS NULL OR date_range_from <= date_range_to),
    CONSTRAINT chk_report_exports_progress_report_pdf_only
        CHECK (report_type <> 'progress_report' OR format = 'pdf'),
    CONSTRAINT chk_report_exports_expiry_after_generation
        CHECK (expires_at > generated_at)
);

-- The exports list (newest first) and the per-user scoping every query applies.
CREATE INDEX idx_report_exports_user_generated ON report_exports (user_id, generated_at DESC);
-- The expiry cleanup job's scan.
CREATE INDEX idx_report_exports_expires_at ON report_exports (expires_at);
