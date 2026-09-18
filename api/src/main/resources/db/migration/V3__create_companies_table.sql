-- Rollback: db/rollback/U3__drop_companies_table.sql

CREATE TABLE companies (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               TEXT NOT NULL,
    normalized_name    TEXT NOT NULL,
    website            TEXT,
    industry           TEXT,
    created_by_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Companies are user-scoped rather than one global table shared by everyone
-- (ADR-004): a global table would leak "who is applying where" via company
-- creation timestamps and needs cross-user dedup logic nobody has time to
-- build correctly. Scoping per user costs some row duplication and buys
-- clean isolation for free, through the same repository base as everything
-- else (PROJECT_PLAN.md §17.2, §17.4). Uniqueness is per user, never global.
CREATE UNIQUE INDEX uq_companies_user_normalized_name ON companies (created_by_user_id, normalized_name);
CREATE INDEX idx_companies_created_by_user_id ON companies (created_by_user_id);
