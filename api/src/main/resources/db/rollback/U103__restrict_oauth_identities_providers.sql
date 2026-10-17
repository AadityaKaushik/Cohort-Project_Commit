DELETE FROM oauth_identities WHERE provider = 'linkedin';
ALTER TABLE oauth_identities DROP CONSTRAINT oauth_identities_provider_check;
ALTER TABLE oauth_identities
    ADD CONSTRAINT oauth_identities_provider_check CHECK (provider IN ('google', 'github'));
