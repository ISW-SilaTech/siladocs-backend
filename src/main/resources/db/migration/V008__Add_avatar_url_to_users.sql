-- Agregar columna avatar_url a tabla users para almacenar la URL de la foto de perfil
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL;
