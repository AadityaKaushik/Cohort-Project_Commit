-- Rollback: db/rollback/U105__drop_fl_round_training_results.sql

-- Rounds now train the shared model, so each round records what came of it.
ALTER TABLE fl.fl_rounds ADD COLUMN sat_out_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fl.fl_rounds ADD COLUMN model_version INTEGER;
ALTER TABLE fl.fl_rounds ADD COLUMN check_accuracy DOUBLE PRECISION;
ALTER TABLE fl.fl_rounds ADD COLUMN noise_multiplier DOUBLE PRECISION;
