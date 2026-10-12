-- Rollback: db/rollback/U22__drop_auth_tokens_and_email_verification.sql

-- US-92: single-use, expiring tokens for password reset and email
-- verification. Only the SHA-256 hash of a token is stored, like refresh
-- tokens, so a database read never yields a usable link.
CREATE TABLE auth_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose    TEXT NOT NULL CHECK (purpose IN ('password_reset', 'email_verification')),
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_tokens_user_purpose ON auth_tokens (user_id, purpose);

ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Accounts that predate verification are treated as verified.
UPDATE users SET email_verified_at = created_at WHERE email_verified_at IS NULL;
