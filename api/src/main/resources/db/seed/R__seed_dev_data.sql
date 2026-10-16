-- Dev seed data (US-72). Ids start with deadbeef and names carry [SEED].
-- Only the dev profile loads db/seed. Safe to run twice (ON CONFLICT DO NOTHING).

-- 1) Demo user. Password is "SeedDemo123!", hashed with the encoder from PasswordEncoderConfig.
INSERT INTO users (
    id, email, password_hash, display_name, target_role, graduation_year, timezone,
    role, is_active, fl_opt_in, fl_privacy_posture, epsilon_spent, created_at, updated_at
) VALUES (
    'deadbeef-0001-4000-8000-000000000001',
    'seed.demo.user@example.com',
    '$argon2id$v=19$m=16384,t=2,p=1$9f1iNohZrstVk1Zf84zM6w$Qei6FRi7ckYMX+O3VTyLI8lynXU41uU+UKynTikFvw0',
    '[SEED] Demo User',
    'Software Engineer',
    2026,
    'Asia/Kolkata',
    'user', true, false, 'balanced', 0,
    now(), now()
)
ON CONFLICT (id) DO NOTHING;

-- 2) Eight companies the demo user has "applied to."
INSERT INTO companies (id, name, normalized_name, website, industry, created_by_user_id, created_at, updated_at)
VALUES
    ('deadbeef-0002-4000-8000-000000000001', '[SEED] Nimbus Systems', '[seed] nimbus systems', 'https://nimbus.example.com', 'Software', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000002', '[SEED] Cascade Analytics', '[seed] cascade analytics', 'https://cascade.example.com', 'Data & Analytics', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000003', '[SEED] Fernbridge Robotics', '[seed] fernbridge robotics', 'https://fernbridge.example.com', 'Robotics', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000004', '[SEED] Solstice Health', '[seed] solstice health', 'https://solstice.example.com', 'Healthcare Tech', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000005', '[SEED] Granite Peak Financial', '[seed] granite peak financial', 'https://granitepeak.example.com', 'Fintech', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000006', '[SEED] Lighthouse Retail Co', '[seed] lighthouse retail co', 'https://lighthouseretail.example.com', 'E-commerce', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000007', '[SEED] Meridian Cloud', '[seed] meridian cloud', 'https://meridiancloud.example.com', 'Cloud Infrastructure', 'deadbeef-0001-4000-8000-000000000001', now(), now()),
    ('deadbeef-0002-4000-8000-000000000008', '[SEED] Orbital Games Studio', '[seed] orbital games studio', 'https://orbitalgames.example.com', 'Gaming', 'deadbeef-0001-4000-8000-000000000001', now(), now())
ON CONFLICT (id) DO NOTHING;

-- 3) Three resumes, the second cloned from the first (US-25).
INSERT INTO resumes (id, user_id, name, version_label, target_role, summary, is_default, parent_resume_id, created_at, updated_at)
VALUES
    ('deadbeef-0005-4000-8000-000000000001', 'deadbeef-0001-4000-8000-000000000001',
     '[SEED] Software Engineer Resume', 'v1', 'Software Engineer', 'General-purpose software engineering resume.', true, NULL, now(), now()),
    ('deadbeef-0005-4000-8000-000000000002', 'deadbeef-0001-4000-8000-000000000001',
     '[SEED] Backend Specialist Resume', 'v2', 'Backend Engineer', 'Tailored towards backend/distributed-systems roles.', false,
     'deadbeef-0005-4000-8000-000000000001', now(), now()),
    ('deadbeef-0005-4000-8000-000000000003', 'deadbeef-0001-4000-8000-000000000001',
     '[SEED] Data-Focused Resume', 'v1', 'Data Analyst', 'Leads with data analysis and SQL projects.', false, NULL, now(), now())
ON CONFLICT (id) DO NOTHING;

-- 4) Forty applications across the eight companies and most statuses.
-- current_stage follows the Application constructor: terminal status keeps stage "saved".
INSERT INTO applications (
    id, user_id, company_id, resume_id, job_title, location, work_mode, job_type,
    priority, is_referral, date_applied, status, current_stage, source,
    stage_entered_at, created_at, updated_at
)
SELECT
    ('deadbeef-0003-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    ('deadbeef-0002-4000-8000-' || lpad((((gs - 1) % 8) + 1)::text, 12, '0'))::uuid,
    CASE WHEN gs <= 20 THEN ('deadbeef-0005-4000-8000-' || lpad((((gs - 1) % 3) + 1)::text, 12, '0'))::uuid ELSE NULL END,
    (ARRAY['Software Engineer Intern', 'Backend Engineer', 'Frontend Engineer', 'Data Analyst',
           'ML Engineer', 'DevOps Engineer', 'Product Manager Intern', 'Full Stack Engineer'])[((gs - 1) % 8) + 1],
    (ARRAY['Bengaluru, IN', 'Remote', 'Hyderabad, IN', 'Pune, IN'])[((gs - 1) % 4) + 1],
    (ARRAY['onsite', 'hybrid', 'remote'])[((gs - 1) % 3) + 1],
    (ARRAY['internship', 'full_time', 'full_time', 'contract'])[((gs - 1) % 4) + 1],
    (ARRAY['low', 'medium', 'high'])[((gs - 1) % 3) + 1],
    (gs % 5 = 0),
    (CURRENT_DATE - ((40 - gs) * 2)),
    status,
    CASE WHEN status IN ('rejected', 'withdrawn', 'accepted', 'declined') THEN 'saved' ELSE status END,
    (ARRAY['LinkedIn', 'Referral', 'Company Website', 'Campus Placement'])[((gs - 1) % 4) + 1],
    (CURRENT_DATE - ((40 - gs) * 2))::timestamptz,
    now(),
    now()
FROM generate_series(1, 40) AS gs
CROSS JOIN LATERAL (
    SELECT (ARRAY['saved', 'applied', 'applied', 'oa', 'oa', 'interview', 'interview',
                  'final_round', 'offer', 'rejected', 'rejected', 'withdrawn',
                  'accepted'])[((gs - 1) % 13) + 1] AS status
) AS s
ON CONFLICT (id) DO NOTHING;

-- 5) One creation history entry per application, as ApplicationService.create() writes.
INSERT INTO application_status_history (id, application_id, from_status, to_status, note, changed_at)
SELECT
    ('deadbeef-0004-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    ('deadbeef-0003-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    NULL,
    (ARRAY['saved', 'applied', 'applied', 'oa', 'oa', 'interview', 'interview',
           'final_round', 'offer', 'rejected', 'rejected', 'withdrawn',
           'accepted'])[((gs - 1) % 13) + 1],
    NULL,
    (CURRENT_DATE - ((40 - gs) * 2))::timestamptz
FROM generate_series(1, 40) AS gs
ON CONFLICT (id) DO NOTHING;

-- 6) Fifteen interviews: ten completed, five upcoming (US-28).
INSERT INTO interviews (
    id, application_id, user_id, scheduled_at, completed_at, round_number, round_type,
    duration_minutes, mode, interviewer_name, interviewer_role, outcome, perceived_difficulty,
    notes, created_at, updated_at
)
SELECT
    ('deadbeef-0006-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    ('deadbeef-0003-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    CASE WHEN gs <= 10 THEN (CURRENT_DATE - (10 - gs))::timestamptz ELSE (CURRENT_DATE + (gs - 10))::timestamptz END,
    CASE WHEN gs <= 10 THEN (CURRENT_DATE - (10 - gs))::timestamptz + interval '45 minutes' ELSE NULL END,
    1,
    (ARRAY['oa', 'phone_screen', 'technical', 'system_design', 'behavioural', 'hr', 'final'])[((gs - 1) % 7) + 1],
    (ARRAY[30, 45, 60])[((gs - 1) % 3) + 1],
    (ARRAY['onsite', 'video', 'phone'])[((gs - 1) % 3) + 1],
    '[SEED] Interviewer ' || gs,
    'Engineering Manager',
    CASE WHEN gs <= 10 THEN (ARRAY['passed', 'failed', 'pending'])[((gs - 1) % 3) + 1] ELSE 'pending' END,
    (((gs - 1) % 5) + 1),
    NULL,
    now(),
    now()
FROM generate_series(1, 15) AS gs
ON CONFLICT (id) DO NOTHING;

-- 7) Two questions per interview (thirty total). self_rating only on completed ones.
INSERT INTO interview_questions (
    id, interview_id, user_id, question_text, category, difficulty, self_rating, notes, created_at, updated_at
)
SELECT
    ('deadbeef-0007-4000-8000-' || lpad(q::text, 12, '0'))::uuid,
    ('deadbeef-0006-4000-8000-' || lpad(interview_index::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    '[SEED] Sample interview question ' || q,
    (ARRAY['dsa', 'technical', 'behavioural', 'hr', 'system_design', 'company_specific'])[((q - 1) % 6) + 1],
    (ARRAY['easy', 'medium', 'hard'])[((q - 1) % 3) + 1],
    CASE WHEN interview_index <= 10 THEN (((q - 1) % 5) + 1) ELSE NULL END,
    NULL,
    now(),
    now()
FROM generate_series(1, 30) AS q
CROSS JOIN LATERAL (SELECT ((q - 1) / 2) + 1 AS interview_index) AS idx
ON CONFLICT (id) DO NOTHING;

-- 8) Debriefs for nine of the ten completed interviews: eight submitted, one draft, one none (US-30).
INSERT INTO interview_feedback (
    id, interview_id, user_id, self_rating, what_went_well, what_went_poorly,
    improvement_areas, is_draft, submitted_at, created_at, updated_at
)
SELECT
    ('deadbeef-0008-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    ('deadbeef-0006-4000-8000-' || lpad(gs::text, 12, '0'))::uuid,
    'deadbeef-0001-4000-8000-000000000001',
    (((gs - 1) % 5) + 1),
    '[SEED] Communicated my approach clearly.',
    CASE WHEN gs = 9 THEN NULL ELSE '[SEED] Spent too long on one sub-problem.' END,
    CASE WHEN gs = 9 THEN NULL ELSE '[SEED] Practice explaining trade-offs faster.' END,
    (gs = 9),
    CASE WHEN gs = 9 THEN NULL ELSE (CURRENT_DATE - (10 - gs))::timestamptz + interval '55 minutes' END,
    now(),
    now()
FROM generate_series(1, 9) AS gs
ON CONFLICT (id) DO NOTHING;
