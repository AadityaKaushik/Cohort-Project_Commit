-- Rollback: db/rollback/U27__drop_offers_table.sql

-- US-100: the terms of an offer received on an application, kept side by
-- side with other offers for comparison. One offer per application. Money is
-- stored as the annual figure in `currency`; equity_value is the user's own
-- estimate of annualised equity, not a market valuation.
CREATE TABLE offers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    application_id    UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    base_salary       NUMERIC NOT NULL CHECK (base_salary >= 0),
    bonus             NUMERIC CHECK (bonus IS NULL OR bonus >= 0),
    equity_value      NUMERIC CHECK (equity_value IS NULL OR equity_value >= 0),
    signing_bonus     NUMERIC CHECK (signing_bonus IS NULL OR signing_bonus >= 0),
    currency          CHAR(3) NOT NULL DEFAULT 'USD',
    location          TEXT,
    work_mode         TEXT CHECK (work_mode IN ('onsite', 'hybrid', 'remote')),
    benefits          TEXT,
    notes             TEXT,
    start_date        DATE,
    response_deadline DATE,
    status            TEXT NOT NULL DEFAULT 'pending'
                          CHECK (status IN ('pending', 'negotiating', 'accepted', 'declined')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_offers_application UNIQUE (application_id)
);

CREATE INDEX idx_offers_user_id ON offers (user_id);
CREATE INDEX idx_offers_user_deadline ON offers (user_id, response_deadline);
