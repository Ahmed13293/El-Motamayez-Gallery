-- ── App users table ─────────────────────────────────────────────────────────
-- Using "app_users" to avoid conflict with Supabase internal auth schema
CREATE TABLE IF NOT EXISTS app_users (
    id         UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    username   TEXT        UNIQUE NOT NULL,
    password   TEXT        NOT NULL,
    role       TEXT        NOT NULL DEFAULT 'user'
                           CHECK (role IN ('admin', 'user')),
    name       TEXT        NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── Seed users ───────────────────────────────────────────────────────────────
INSERT INTO app_users (username, password, role, name)
VALUES
    ('admin',  'admin123', 'admin', 'المدير'),
    ('user1',  'user123',  'user',  'مستخدم')
ON CONFLICT (username) DO NOTHING;
