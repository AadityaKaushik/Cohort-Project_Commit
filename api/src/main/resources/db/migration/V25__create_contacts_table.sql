-- Rollback: db/rollback/U25__drop_contacts_table.sql

-- US-95: people a user knows at or around a company -- recruiters, referrers,
-- peers -- with the referral state of each. Strictly user-owned. The company
-- and application links are optional and SET NULL, so deleting either never
-- deletes a person the user still wants to keep track of.
--
-- last_contacted_at drives the cold-contact nudge: a contact not touched for
-- a while is reported through the Observer events like every other nudge.
CREATE TABLE contacts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id        UUID REFERENCES companies (id) ON DELETE SET NULL,
    application_id    UUID REFERENCES applications (id) ON DELETE SET NULL,
    name              TEXT NOT NULL,
    role_title        TEXT,
    email             TEXT,
    linkedin_url      TEXT,
    relationship      TEXT NOT NULL DEFAULT 'other'
                          CHECK (relationship IN ('recruiter', 'hiring_manager', 'referrer', 'peer', 'alumni', 'other')),
    referral_status   TEXT NOT NULL DEFAULT 'none'
                          CHECK (referral_status IN ('none', 'requested', 'agreed', 'submitted', 'declined')),
    notes             TEXT,
    last_contacted_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contacts_user_id ON contacts (user_id);
CREATE INDEX idx_contacts_user_company ON contacts (user_id, company_id);
CREATE INDEX idx_contacts_user_application ON contacts (user_id, application_id);

-- The cold-contact nudge is a new notification kind that points at a contact.
-- Widen the V20 CHECK constraints to admit both.
ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('stalled_application', 'upcoming_interview', 'missing_debrief',
                    'new_recommendation', 'fl_round_result', 'cold_contact'));
ALTER TABLE notifications DROP CONSTRAINT notifications_related_entity_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_related_entity_type_check
    CHECK (related_entity_type IN ('application', 'interview', 'contact'));
