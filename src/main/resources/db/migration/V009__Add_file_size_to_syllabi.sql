-- ============================================================================
-- Migración: V009__Add_file_size_to_syllabi.sql
-- Propósito: Persistir el tamaño (bytes) del archivo actual de cada sílabo.
-- Antes este valor no se guardaba y las respuestas de listado/detalle
-- siempre devolvían 0, mostrando "Tamaño: —" en el frontend.
-- ============================================================================

ALTER TABLE syllabi
    ADD COLUMN IF NOT EXISTS file_size BIGINT NULL;
