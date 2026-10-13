-- Rollback: db/rollback/U23__drop_onboarding_from_users.sql

-- US-91: first-run onboarding. `onboarded_at` is null until a new user
-- completes (or skips) the starter plan; `target_date` is the day they want
-- to be interview-ready by and spreads the seeded preparation tasks.
ALTER TABLE users ADD COLUMN target_date DATE;
ALTER TABLE users ADD COLUMN onboarded_at TIMESTAMPTZ;

-- Accounts that predate onboarding must never be sent through it.
UPDATE users SET onboarded_at = now() WHERE onboarded_at IS NULL;
