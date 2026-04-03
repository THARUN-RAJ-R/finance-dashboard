-- =============================================================
-- V2__seed_roles_and_admin.sql  –  Seed roles + default admin
-- =============================================================

-- ── Roles ─────────────────────────────────────────────────────
INSERT INTO roles (name) VALUES
    ('VIEWER'),
    ('ANALYST'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- ── Default categories ────────────────────────────────────────
INSERT INTO categories (name) VALUES
    ('Salary'),
    ('Rent'),
    ('Utilities'),
    ('Groceries'),
    ('Travel'),
    ('Healthcare'),
    ('Entertainment'),
    ('Investment'),
    ('Freelance'),
    ('Miscellaneous')
ON CONFLICT (name) DO NOTHING;

-- ── Admin user (admin@local / admin123) ───────────────────────
-- BCrypt hash of "admin123" with strength 10
INSERT INTO users (id, email, password_hash, status)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@finance.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id
FROM roles WHERE name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ── Demo analyst user (analyst@local / analyst123) ────────────
INSERT INTO users (id, email, password_hash, status)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'analyst@finance.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT 'a0000000-0000-0000-0000-000000000002', id
FROM roles WHERE name = 'ANALYST'
ON CONFLICT DO NOTHING;

-- ── Demo viewer user (viewer@local / viewer123) ───────────────
INSERT INTO users (id, email, password_hash, status)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'viewer@finance.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT 'a0000000-0000-0000-0000-000000000003', id
FROM roles WHERE name = 'VIEWER'
ON CONFLICT DO NOTHING;
