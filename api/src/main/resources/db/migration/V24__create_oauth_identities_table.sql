-- Rollback: db/rollback/U24__drop_oauth_identities_table.sql

-- US-93: an external identity (Google or GitHub account) linked to a user.
-- (provider, subject) is the stable key; the email is kept only for display
-- and is never used to look an identity up, because provider emails change.
CREATE TABLE oauth_identities (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider   TEXT NOT NULL CHECK (provider IN ('google', 'github')),
    subject    TEXT NOT NULL,
    email      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_oauth_identity UNIQUE (provider, subject)
);

CREATE INDEX idx_oauth_identities_user_id ON oauth_identities (user_id);
