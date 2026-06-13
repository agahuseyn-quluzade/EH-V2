\connect ehi_iam
INSERT INTO users (id, email, password, first_name, last_name, phone, role, active, created_at, updated_at) VALUES
  (gen_random_uuid(), 'admin@ehi.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Admin', 'User', '+994501234567', 'ADMIN', true, now(), now()),
  (gen_random_uuid(), 'staff@ehi.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Staff', 'User', '+994502234567', 'STAFF', true, now(), now()),
  (gen_random_uuid(), 'john.doe@gmail.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'John', 'Doe', '+994503334567', 'CUSTOMER', true, now(), now()),
  (gen_random_uuid(), 'emily.smith@gmail.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Emily', 'Smith', '+994504434567', 'CUSTOMER', true, now(), now()),
  (gen_random_uuid(), 'james.wilson@gmail.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'James', 'Wilson', '+994505534567', 'CUSTOMER', true, now(), now())
ON CONFLICT (email) DO NOTHING;

\connect ehi_policy
INSERT INTO plans (id, name, description, coverage_amount, premium_amount, duration_months, active, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Basic Health Plan', 'Essential coverage for individuals including general practitioner visits, emergency care, and basic diagnostics.', 10000.00, 599.88, 12, true, now(), now()),
  (gen_random_uuid(), 'Standard Health Plan', 'Comprehensive coverage including specialist visits, hospitalization, lab tests, and prescription medications.', 25000.00, 1199.88, 12, true, now(), now()),
  (gen_random_uuid(), 'Premium Health Plan', 'Full coverage including dental, vision, mental health, specialist care, and international emergency coverage.', 50000.00, 2399.88, 12, true, now(), now()),
  (gen_random_uuid(), 'Family Health Plan', 'All-inclusive family coverage for up to 5 members with maternity, pediatric care, and chronic disease management.', 100000.00, 4199.88, 12, true, now(), now())
ON CONFLICT (name) DO NOTHING;
