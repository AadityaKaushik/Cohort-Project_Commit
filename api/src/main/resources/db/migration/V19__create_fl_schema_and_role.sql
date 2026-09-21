-- Rollback: db/rollback/U19__drop_fl_schema_and_role.sql

-- P50 (US-58..64): the isolated schema + least-privilege Postgres role
-- fl-service will (if it ever writes directly to Postgres) authenticate
-- as. Per ARCHITECTURE_REVISION_JAVA.md §3's diagram: an INSERT-only role,
-- scoped to exactly two bookkeeping tables (fl_rounds, privacy_ledger_entries),
-- with zero access to applications/resumes/interviews/notes or any other
-- user-content table. This migration creates the schema, the two tables,
-- and the role/grants together so the isolation guarantee is established
-- in the same commit the tables are, not bolted on later.
--
-- In this batch's actual implementation, fl-service never opens a direct
-- Postgres connection at all (see FederatedLearningGateway/P52-55): Java,
-- which already holds full DB credentials as the app owner, is what
-- persists round/ledger rows, fed by fl-service's REST responses. That is
-- a *stronger* isolation guarantee than "restricted but present" credentials
-- -- but the role is still provisioned and CI-verified here (P50's own
-- verify requirement) as least-privilege scaffolding for the day fl-service
-- does write directly, and so the isolation property is provable against a
-- real running Postgres instance right now, not just asserted in prose.
CREATE SCHEMA IF NOT EXISTS fl;

CREATE TABLE fl.fl_rounds (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_number  INTEGER NOT NULL,
    accepted      BOOLEAN NOT NULL,
    reason        TEXT,
    cohort_size   INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- US-63: "one entry per round: round ID, timestamp, schema version,
-- pre/post-clip norm, noise multiplier, cohort size, epsilon consumed, and
-- the exact transmitted vector". `transmitted_vector` holds the REAL
-- post-clip-post-noise floats fl-service's privacy module (P46) produced
-- for that client in that round -- never a placeholder (P55).
CREATE TABLE fl.privacy_ledger_entries (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id           UUID NOT NULL REFERENCES fl.fl_rounds (id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    schema_version     INTEGER NOT NULL,
    pre_clip_norm      DOUBLE PRECISION NOT NULL,
    post_clip_norm     DOUBLE PRECISION NOT NULL,
    noise_multiplier   DOUBLE PRECISION NOT NULL,
    cohort_size        INTEGER NOT NULL,
    epsilon            DOUBLE PRECISION NOT NULL,
    transmitted_vector JSONB NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_privacy_ledger_user ON fl.privacy_ledger_entries (user_id, created_at DESC);
CREATE INDEX idx_privacy_ledger_round ON fl.privacy_ledger_entries (round_id);

-- The role itself. Password comes from the `flPassword` Flyway placeholder
-- (application.yml: spring.flyway.placeholders.flPassword, sourced from the
-- FL_POSTGRES_PASSWORD env var / .env) rather than a literal secret
-- committed here. IF NOT EXISTS-guarded so this migration stays idempotent
-- if ever re-run against a database that already has the role.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'cohort_fl') THEN
        CREATE ROLE cohort_fl LOGIN PASSWORD '${flPassword}';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA fl TO cohort_fl;
GRANT SELECT, INSERT ON fl.fl_rounds, fl.privacy_ledger_entries TO cohort_fl;

-- Explicit, not merely absent: cohort_fl gets nothing on the schema that
-- holds actual user content. Redundant with "never granted" today, but
-- makes the isolation property survive a future `GRANT ... TO PUBLIC`
-- mistake on the app schema instead of silently depending on one never
-- happening.
REVOKE ALL ON SCHEMA public FROM cohort_fl;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cohort_fl;
