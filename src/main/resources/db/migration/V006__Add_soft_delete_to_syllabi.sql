-- ============================================================================
-- Migración: V006__Add_soft_delete_to_syllabi.sql
-- Propósito: Soportar HU0010 (eliminar sílabo manteniendo trazabilidad blockchain).
-- El sílabo desaparece de los listados activos pero su registro, hash y
-- versiones permanecen intactos para auditoría.
-- ============================================================================

ALTER TABLE syllabi
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE syllabi
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE syllabi
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255) NULL;
