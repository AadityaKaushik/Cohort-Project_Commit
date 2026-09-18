-- Rollback: db/rollback/U8__drop_resume_sections_table.sql

-- One flexible table rather than five near-identical ones (experience,
-- education, project, achievement, skill) — the sections share a shape and
-- JSONB metadata absorbs the differences (PROJECT_PLAN.md §17.2 design note).
CREATE TABLE resume_sections (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id    UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    section_type TEXT NOT NULL
                     CHECK (section_type IN ('experience', 'education', 'project', 'achievement', 'skill')),
    title        TEXT,
    organisation TEXT,
    start_date   DATE,
    end_date     DATE,
    description  TEXT,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    metadata     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_resume_sections_resume_id ON resume_sections (resume_id, sort_order);
