-- Default seed data for SaglamOl. Idempotent (ON CONFLICT DO NOTHING) — safe to re-run.
-- Run by the `seed` service in docker-compose after the services' Liquibase migrations.

-- Default admin account ─ email: admin@saglamol.com  /  password: AdminPass123
\connect ehi_iam
INSERT INTO users (id, email, password, first_name, last_name, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@saglamol.com',
    '$2a$10$hn7A579reqoje.1S95MqIus7PQQUKCVUY0EqoOwS8QxsMkS56ph1.',  -- bcrypt("AdminPass123")
    'Site', 'Admin', 'ADMIN', true, now(), now()
)
ON CONFLICT (email) DO NOTHING;

-- Default insurance plans
\connect ehi_policy
INSERT INTO plans (id, name, description, coverage_amount, premium_amount, duration_months, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Basic Plan',    'Essential yearly health coverage for individuals.', 5000,  300,  12, true, now(), now()),
    (gen_random_uuid(), 'Standard Plan', 'Broader coverage for individuals and families.',     15000, 700,  12, true, now(), now()),
    (gen_random_uuid(), 'Gold Plan',     'Comprehensive full coverage with higher limits.',    50000, 1200, 12, true, now(), now())
ON CONFLICT (name) DO NOTHING;
