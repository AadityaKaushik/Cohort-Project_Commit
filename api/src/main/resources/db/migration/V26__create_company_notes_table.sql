-- Rollback: db/rollback/U26__drop_company_notes_table.sql

-- US-96: free-form notes a user keeps about a company (culture, interview
-- tips, things to ask). Strictly user-owned; cascades with the company and
-- with the owner. Several notes per company, newest first.
CREATE TABLE company_notes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_company_notes_user_company ON company_notes (user_id, company_id, created_at DESC);
