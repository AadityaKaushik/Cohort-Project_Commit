-- Rollback: db/rollback/U1__drop_users_table.sql

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              CITEXT NOT NULL UNIQUE,
    password_hash      TEXT NOT NULL,
    display_name       TEXT,
    target_role        TEXT,
    graduation_year    SMALLINT,
    timezone           TEXT,
    role               TEXT NOT NULL DEFAULT 'user'
                           CHECK (role IN ('user', 'admin')),
    is_active          BOOLEAN NOT NULL DEFAULT true,
    fl_opt_in          BOOLEAN NOT NULL DEFAULT false,
    fl_privacy_posture TEXT NOT NULL DEFAULT 'balanced'
                           CHECK (fl_privacy_posture IN ('strict', 'balanced', 'contributor')),
    epsilon_spent      NUMERIC NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
