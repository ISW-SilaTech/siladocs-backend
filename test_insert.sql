INSERT INTO access_codes (id, code, institution_name, expires_at, used, created_at) 
VALUES (gen_random_uuid(), 'TEST-CODE-001', 'Test Institution', NOW() + INTERVAL '1 day', FALSE, NOW());
