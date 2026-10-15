-- No user id on purpose. contributor_hash is an HMAC of the user id and the company id,
-- so it can be compared inside one company but not joined back to an account.
CREATE TABLE experience_contributions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    contributor_hash    TEXT NOT NULL,
    role_family         TEXT NOT NULL,
    level               TEXT NOT NULL CHECK (level IN ('fresher', 'experienced')),
    contribution_year   INTEGER NOT NULL CHECK (contribution_year BETWEEN 2000 AND 2100),
    rounds              JSONB NOT NULL DEFAULT '[]'::jsonb,
    overall_difficulty  SMALLINT NOT NULL CHECK (overall_difficulty BETWEEN 1 AND 5),
    overall_experience  SMALLINT NOT NULL CHECK (overall_experience BETWEEN 1 AND 5),
    outcome             TEXT NOT NULL CHECK (outcome IN ('offer', 'rejected', 'withdrawn', 'pending')),
    tip                 TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, role_family, contributor_hash, contribution_year)
);

CREATE INDEX idx_experience_contributions_company ON experience_contributions (company_id, role_family);
CREATE INDEX idx_experience_contributions_hash ON experience_contributions (contributor_hash);
