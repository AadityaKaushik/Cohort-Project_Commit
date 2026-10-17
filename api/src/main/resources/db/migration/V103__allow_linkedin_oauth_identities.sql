-- Rollback: db/rollback/U103__restrict_oauth_identities_providers.sql

-- LinkedIn sign-in stores its identities next to Google and GitHub ones.
ALTER TABLE oauth_identities DROP CONSTRAINT oauth_identities_provider_check;
ALTER TABLE oauth_identities
    ADD CONSTRAINT oauth_identities_provider_check CHECK (provider IN ('google', 'github', 'linkedin'));
