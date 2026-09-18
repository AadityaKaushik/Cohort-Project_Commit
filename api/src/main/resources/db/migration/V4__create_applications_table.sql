-- Rollback: db/rollback/U4__drop_applications_table.sql

CREATE TABLE applications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id       UUID NOT NULL REFERENCES companies (id),
    -- The resumes table doesn't exist until the Sprint 2 resumes migration;
    -- its FK constraint is added there (ALTER TABLE ... ADD CONSTRAINT)
    -- once the target table exists. The column exists now so FR-34
    -- ("associate exactly one resume with an application") doesn't need a
    -- later column-add migration, only a later constraint-add one.
    resume_id        UUID,
    job_title        TEXT NOT NULL,
    job_description  TEXT,
    job_url          TEXT,
    location         TEXT,
    work_mode        TEXT CHECK (work_mode IN ('onsite', 'hybrid', 'remote')),
    job_type         TEXT CHECK (job_type IN ('internship', 'full_time', 'part_time', 'contract')),
    salary_min       NUMERIC,
    salary_max       NUMERIC,
    salary_currency  CHAR(3),
    date_applied     DATE NOT NULL,
    -- Full status domain including terminal outcomes (§17.5 state machine).
    status           TEXT NOT NULL DEFAULT 'saved'
                         CHECK (status IN ('saved', 'applied', 'oa', 'interview', 'final_round',
                                            'offer', 'rejected', 'withdrawn', 'accepted', 'declined')),
    -- The furthest *non-terminal* stage this application has ever reached,
    -- independent of its current (possibly terminal) status. This is what
    -- makes "ever-reached stage" funnel analytics (FR-59) and dwell-time
    -- stall flags (FR-25/26) correct: an application Rejected right after
    -- the Interview round must still count as having reached Interview,
    -- which `status` alone (now 'rejected') can no longer tell you.
    current_stage    TEXT NOT NULL DEFAULT 'saved'
                         CHECK (current_stage IN ('saved', 'applied', 'oa', 'interview', 'final_round', 'offer')),
    priority         TEXT CHECK (priority IN ('low', 'medium', 'high')),
    source           TEXT,
    is_referral      BOOLEAN NOT NULL DEFAULT false,
    deadline         DATE,
    recruiter_name   TEXT,
    recruiter_email  TEXT,
    notes            TEXT,
    cover_letter     TEXT,
    archived_at      TIMESTAMPTZ,
    stage_entered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_applications_salary_range
        CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max),
    CONSTRAINT chk_applications_date_applied_not_future
        CHECK (date_applied <= CURRENT_DATE)
);

-- Deliberately no uniqueness on (user_id, company_id) — a user may hold
-- several concurrent applications with the same company (DR-03).
CREATE INDEX idx_applications_user_status ON applications (user_id, status);
CREATE INDEX idx_applications_user_date_applied ON applications (user_id, date_applied DESC);
CREATE INDEX idx_applications_user_company ON applications (user_id, company_id);
CREATE INDEX idx_applications_user_archived ON applications (user_id, archived_at);
