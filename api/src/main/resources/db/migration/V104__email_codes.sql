-- Rollback: db/rollback/U104__drop_email_code_attempts.sql

-- Emailed codes allow a few wrong guesses before they stop working.
ALTER TABLE auth_tokens ADD COLUMN failed_attempts integer NOT NULL DEFAULT 0;

-- Outstanding emailed links are replaced by codes.
UPDATE auth_tokens SET used_at = now() WHERE used_at IS NULL;

-- Accounts that exist when sign-in starts to require a confirmed email keep working.
UPDATE users SET email_verified_at = now() WHERE email_verified_at IS NULL;
