-- Create access_codes table
CREATE TABLE IF NOT EXISTS access_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(255) UNIQUE NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indices for performance
CREATE INDEX IF NOT EXISTS idx_access_codes_code ON access_codes(code);
CREATE INDEX IF NOT EXISTS idx_access_codes_used ON access_codes(used);
CREATE INDEX IF NOT EXISTS idx_access_codes_expires_at ON access_codes(expires_at);

-- Insert test data
INSERT INTO access_codes (id, code, institution_name, expires_at, used, created_at)
VALUES (
    gen_random_uuid(),
    'TEST-CODE-001',
    'Test Institution',
    NOW() + INTERVAL '1 day',
    FALSE,
    NOW()
) ON CONFLICT (code) DO NOTHING;

-- Verify insert
SELECT * FROM access_codes WHERE code = 'TEST-CODE-001';
