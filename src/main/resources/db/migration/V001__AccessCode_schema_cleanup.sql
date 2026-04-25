-- ============================================================================
-- Migración: V001__AccessCode_schema_cleanup.sql
-- Propósito: Inicialización de la tabla AccessCode y sus índices
-- ============================================================================

-- 1. Creamos la tabla explícitamente con Flyway
CREATE TABLE IF NOT EXISTS access_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Creamos los índices
CREATE INDEX IF NOT EXISTS idx_access_codes_code ON access_codes(code);
CREATE INDEX IF NOT EXISTS idx_access_codes_used ON access_codes(used);
CREATE INDEX IF NOT EXISTS idx_access_codes_expires_at ON access_codes(expires_at);
