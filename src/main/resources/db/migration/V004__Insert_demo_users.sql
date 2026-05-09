-- ============================================================================
-- Migración: V004__Insert_demo_users.sql
-- Propósito: Insertar institución y usuarios demo para validación del sistema
-- ============================================================================

-- 1. Insertar institución demo (si no existe)
INSERT INTO institutions (name, domain, status, created_at)
SELECT 'Universidad Demo - SilaDocs', 'demo.siladocs.com', 'active', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM institutions WHERE domain = 'demo.siladocs.com'
);

-- 2. Insertar usuario Rector demo (si no existe)
INSERT INTO users (name, email, password_hash, role, institution_id, created_at)
SELECT
    'Rector Demo',
    'rector@demo.siladocs.com',
    '$2b$10$8jqlFAxuLZlQ9Rk2ero7geBxVjGyptITNPtAY.rG/xWHsTi1NFqii',
    'Rector',
    (SELECT institution_id FROM institutions WHERE domain = 'demo.siladocs.com'),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'rector@demo.siladocs.com'
);

-- 3. Insertar usuario Administrador Académico demo (si no existe)
INSERT INTO users (name, email, password_hash, role, institution_id, created_at)
SELECT
    'Administrador Académico Demo',
    'academico@demo.siladocs.com',
    '$2b$10$3lUmwjbr4gudjgK5Pu1XCOQPUWDGIYJCCXMX4e9OhS4BidETCLHtq',
    'Administrador Académico',
    (SELECT institution_id FROM institutions WHERE domain = 'demo.siladocs.com'),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'academico@demo.siladocs.com'
);
