-- Rollback: db/rollback/U9__drop_interviews_table.sql

CREATE TABLE interviews (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id        UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    -- Denormalised alongside application_id, not resolved through a join, so
    -- every isolation query against this table is a single-table filter and
    -- can never be forgotten in a join (US-27, PROJECT_PLAN.md §17.4;
    -- UserScopedRepository's own doc comment already names this table as
    -- the reason it requires a direct userId property on every entity).
    user_id               UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scheduled_at          TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    round_number          SMALLINT NOT NULL CHECK (round_number > 0),
    round_type            TEXT NOT NULL
                              CHECK (round_type IN ('oa', 'phone_screen', 'technical', 'system_design',
                                                     'behavioural', 'hr', 'final')),
    duration_minutes      INTEGER,
    mode                  TEXT CHECK (mode IN ('onsite', 'video', 'phone')),
    -- DR-11: interviewer identity is a third party's personal data with no
    -- consent on file. Free text, no separate entity, excluded from all
    -- analytics and FL features (neither ever reads these columns), and
    -- purged whenever the owning interview/application is deleted via the
    -- FK cascades above.
    interviewer_name      TEXT,
    interviewer_role      TEXT,
    outcome               TEXT NOT NULL DEFAULT 'pending'
                              CHECK (outcome IN ('passed', 'failed', 'pending', 'unknown')),
    perceived_difficulty  SMALLINT CHECK (perceived_difficulty BETWEEN 1 AND 5),
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- DR-04: a variable number of rounds per application without a fixed
    -- count — enforced unique per application, never globally.
    CONSTRAINT uq_interviews_application_round UNIQUE (application_id, round_number)
);

CREATE INDEX idx_interviews_user_id ON interviews (user_id);
CREATE INDEX idx_interviews_application_id ON interviews (application_id, round_number);
CREATE INDEX idx_interviews_user_scheduled_at ON interviews (user_id, scheduled_at);
