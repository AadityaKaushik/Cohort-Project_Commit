CREATE TABLE company_profiles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL,
    slug          TEXT NOT NULL UNIQUE,
    industry      TEXT,
    locations     TEXT,
    size_band     TEXT,
    website       TEXT,
    summary       TEXT,
    sources       JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_checked  DATE NOT NULL
);

CREATE INDEX idx_company_profiles_name_lower ON company_profiles (lower(name));

CREATE TABLE company_role_patterns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES company_profiles (id) ON DELETE CASCADE,
    role_family         TEXT NOT NULL,
    level               TEXT NOT NULL,
    rounds              JSONB NOT NULL DEFAULT '[]'::jsonb,
    topic_slugs         JSONB NOT NULL DEFAULT '[]'::jsonb,
    notes               TEXT,
    experience_summary  TEXT,
    pay                 JSONB,
    sources             JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_checked        DATE NOT NULL,
    UNIQUE (company_id, role_family, level)
);
