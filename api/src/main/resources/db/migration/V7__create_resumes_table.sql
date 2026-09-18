-- Rollback: db/rollback/U7__drop_resumes_table.sql

CREATE TABLE resumes (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name               TEXT NOT NULL,
    version_label      TEXT,
    target_role        TEXT,
    summary            TEXT,
    is_default         BOOLEAN NOT NULL DEFAULT false,
    -- Lineage for cloning (FR-35, US-25): the resume this one was cloned
    -- from, or NULL for an originally-created resume. ON DELETE SET NULL so
    -- deleting a parent never cascades into deleting every clone made from it.
    parent_resume_id   UUID REFERENCES resumes (id) ON DELETE SET NULL,
    -- File fields are all nullable: a resume record can exist before a file
    -- is uploaded to it (US-20 creates the record, US-21 uploads the file).
    file_path          TEXT,
    file_original_name TEXT,
    file_size_bytes    BIGINT,
    file_mime          TEXT,
    file_uploaded_at   TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Exactly one default resume per user (FR-33). A partial unique index is
-- the standard Postgres idiom for "at most one TRUE per group" since there
-- is no direct constraint for it.
CREATE UNIQUE INDEX uq_resumes_user_default ON resumes (user_id) WHERE is_default;

CREATE INDEX idx_resumes_user_id ON resumes (user_id);
CREATE INDEX idx_resumes_parent_resume_id ON resumes (parent_resume_id);

-- The applications.resume_id column has existed since V4; its FK constraint
-- was deferred until this migration, which is the first point the
-- referenced table exists (see the comment in V4__create_applications_table.sql).
ALTER TABLE applications
    ADD CONSTRAINT fk_applications_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE SET NULL;
