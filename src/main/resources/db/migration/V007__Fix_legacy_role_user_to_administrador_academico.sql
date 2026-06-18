-- ============================================================================
-- Migración: V007__Fix_legacy_role_user_to_administrador_academico.sql
-- Propósito: Corregir usuarios registrados antes de este fix, que quedaron con
-- el rol literal "ROLE_USER" en vez de "Administrador Académico".
--
-- AuthService.registerInstitution() asignaba "ROLE_USER" a todo usuario que se
-- registraba con un código de acceso institucional. Como ese código es de un
-- solo uso, dicho usuario es el administrador principal de su institución y
-- debe tener el rol "Administrador Académico" (literal usado por los permisos
-- de HU0010 y otros, p.ej. eliminar sílabos). Esta migración corrige los datos
-- de los usuarios ya registrados antes de que se corrigiera el código.
-- ============================================================================

UPDATE users
SET role = 'Administrador Académico'
WHERE role = 'ROLE_USER';
