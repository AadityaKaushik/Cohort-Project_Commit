-- Rollback: db/rollback/U100__drop_offer_scoring_tables.sql

-- US-100: the weights a user puts on each decision factor (1 to 5) and how
-- they rate each offer on those factors (1 to 5). A missing weight means the
-- default of 3. Scores are computed from these rows, never stored.
CREATE TABLE offer_factor_weights (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    factor     TEXT NOT NULL
                   CHECK (factor IN ('pay', 'growth', 'learning', 'work_life_balance', 'location', 'brand')),
    weight     SMALLINT NOT NULL CHECK (weight BETWEEN 1 AND 5),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_offer_factor_weights UNIQUE (user_id, factor)
);

CREATE TABLE offer_ratings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    offer_id   UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    factor     TEXT NOT NULL
                   CHECK (factor IN ('pay', 'growth', 'learning', 'work_life_balance', 'location', 'brand')),
    rating     SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_offer_ratings UNIQUE (offer_id, factor)
);

CREATE INDEX idx_offer_ratings_user_id ON offer_ratings (user_id);
