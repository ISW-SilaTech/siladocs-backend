-- ============================================================================
-- Migración: V001__AccessCode_schema_cleanup.sql
-- Propósito: Refactorización arquitectónica de AccessCode
-- - Consolidar entidad única JPA en domain/entity/AccessCode
-- - Eliminar duplicaciones (AccessCodeEntity, AccessCodeJpaRepository, Mapper)
-- - Cambiar timestamps de LocalDateTime a Instant (almacenado como TIMESTAMP)
-- - Crear repositorio JPA directo en domain/repository/
--
-- Descripción técnica:
--   * La tabla access_codes mantiene su estructura actual
--   * Los campos created_at y expires_at usan TIMESTAMP (Compatible con Instant)
--   * Hibernate autodiscubre la entidad de domain/entity/AccessCode
--   * El repositorio JpaRepository está directamente en domain/repository/
-- ============================================================================

-- Verificar que la tabla access_codes existe (creada por Hibernate si no existe)
-- Si necesita crear manualmente:
-- CREATE TABLE IF NOT EXISTS access_codes (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     code VARCHAR(255) UNIQUE NOT NULL,
--     institution_name VARCHAR(255) NOT NULL,
--     expires_at TIMESTAMP NOT NULL,
--     used BOOLEAN NOT NULL DEFAULT FALSE,
--     created_at TIMESTAMP NOT NULL DEFAULT NOW()
-- );

-- Crear índice para búsquedas por código (mejora rendimiento de findByCode)
CREATE INDEX IF NOT EXISTS idx_access_codes_code ON access_codes(code);

-- Crear índice para búsquedas por estado de uso
CREATE INDEX IF NOT EXISTS idx_access_codes_used ON access_codes(used);

-- Crear índice para búsquedas por expiración
CREATE INDEX IF NOT EXISTS idx_access_codes_expires_at ON access_codes(expires_at);

-- ============================================================================
-- Fin de la migración
-- ============================================================================
