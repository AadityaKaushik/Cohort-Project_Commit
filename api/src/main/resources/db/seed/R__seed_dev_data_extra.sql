-- Dev seed rows for contacts, offers and goals, owned by the demo user from R__seed_dev_data.sql.
-- Runs after the base seed (repeatables run in description order).

INSERT INTO contacts (
    id, user_id, company_id, application_id, name, role_title, email, relationship,
    referral_status, notes, last_contacted_at, created_at, updated_at
)
SELECT
    ('deadbeef-0009-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    ('deadbeef-0002-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    ('deadbeef-0003-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    '[SEED] Contact ' || gs,
    (ARRAY['Recruiter', 'Engineering Manager', 'Senior Engineer', 'Alumni Mentor', 'Talent Partner', 'Staff Engineer'])[gs],
    'contact' || gs || '@example.com',
    (ARRAY['recruiter', 'hiring_manager', 'referrer', 'alumni', 'recruiter', 'peer'])[gs],
    (ARRAY['none', 'none', 'requested', 'agreed', 'none', 'submitted'])[gs],
    '[SEED] Met through a campus event.',
    now() - ((gs * 9) || ' days')::interval,
    now(),
    now()
FROM generate_series(1, 6) AS gs
ON CONFLICT (id) DO NOTHING;

-- Applications 9, 22 and 35 are the ones the base seed puts in status 'offer'.
INSERT INTO offers (
    id, user_id, application_id, base_salary, bonus, equity_value, currency, location,
    work_mode, notes, start_date, response_deadline, status, created_at, updated_at
)
SELECT
    ('deadbeef-000a-4000-8000-' || lpad(idx::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    ('deadbeef-0003-4000-8000-' || lpad(app::text, 12, '0'))::uuid,
    (ARRAY[1800000, 2200000, 1500000])[idx],
    (ARRAY[150000, 200000, NULL])[idx],
    (ARRAY[NULL, 400000, 0])[idx],
    'INR',
    (ARRAY['Bengaluru, IN', 'Remote', 'Pune, IN'])[idx],
    (ARRAY['hybrid', 'remote', 'onsite'])[idx],
    '[SEED] Offer letter received by email.',
    CURRENT_DATE + 60,
    CURRENT_DATE + (ARRAY[5, 12, 20])[idx],
    (ARRAY['pending', 'negotiating', 'pending'])[idx],
    now(),
    now()
FROM unnest(ARRAY[9, 22, 35]) WITH ORDINALITY AS t(app, idx)
ON CONFLICT (id) DO NOTHING;

INSERT INTO goals (id, user_id, metric, weekly_target, created_at, updated_at)
VALUES
    ('deadbeef-000b-4000-8000-000000000001', 'deadbeef-0001-4000-8000-000000000001', 'applications', 5, now(), now()),
    ('deadbeef-000b-4000-8000-000000000002', 'deadbeef-0001-4000-8000-000000000001', 'interviews', 2, now(), now()),
    ('deadbeef-000b-4000-8000-000000000003', 'deadbeef-0001-4000-8000-000000000001', 'practice_attempts', 10, now(), now()),
    ('deadbeef-000b-4000-8000-000000000004', 'deadbeef-0001-4000-8000-000000000001', 'contacts_reached', 3, now(), now())
ON CONFLICT (id) DO NOTHING;
