-- ============================================================================
-- Migración: V010__Insert_rbac_test_users.sql
-- Propósito: Insertar 4 usuarios de prueba (uno por rol) para validar el
-- control de acceso por roles (RBAC) desde el frontend. Todos asociados a la
-- institución existente "Universidad Peruana de Ciencias Aplicadas".
--
-- Password para los 4 usuarios: Test1234!  (hash bcrypt, costo 10)
-- ============================================================================
INSERT INTO
    users (
        name,
        email,
        password_hash,
        role,
        institution_id,
        created_at
    )
SELECT
    'Admin Académico (Prueba RBAC)',
    'admin.academico@siladocs.test',
    '$2b$10$yNrVlBvVC0MgFr45SNIqzebUyvXD8vy7fj79GoMA1uCbFHC/Eel8O',
    'Administrador Académico',
    (
        SELECT
            institution_id
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
        LIMIT
            1
    ),
    NOW ()
WHERE
    EXISTS (
        SELECT
            1
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
    )
    AND NOT EXISTS (
        SELECT
            1
        FROM
            users
        WHERE
            email = 'admin.academico@siladocs.test'
    );

INSERT INTO
    users (
        name,
        email,
        password_hash,
        role,
        institution_id,
        created_at
    )
SELECT
    'Rector (Prueba RBAC)',
    'rector@siladocs.test',
    '$2b$10$yixV26Oua780UfRxoXoXU.Byd1a.50h2jPnIERZBrC5Y1/bCoE6/.',
    'Rector',
    (
        SELECT
            institution_id
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
        LIMIT
            1
    ),
    NOW ()
WHERE
    EXISTS (
        SELECT
            1
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
    )
    AND NOT EXISTS (
        SELECT
            1
        FROM
            users
        WHERE
            email = 'rector@siladocs.test'
    );

INSERT INTO
    users (
        name,
        email,
        password_hash,
        role,
        institution_id,
        created_at
    )
SELECT
    'Docente (Prueba RBAC)',
    'docente@siladocs.test',
    '$2b$10$YzrxUr4Y9McbIH8/CgMSlu8OaaxgmmzaEejAxL3jGWqisoiUFQfGO',
    'Docente',
    (
        SELECT
            institution_id
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
        LIMIT
            1
    ),
    NOW ()
WHERE
    EXISTS (
        SELECT
            1
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
    )
    AND NOT EXISTS (
        SELECT
            1
        FROM
            users
        WHERE
            email = 'docente@siladocs.test'
    );

INSERT INTO
    users (
        name,
        email,
        password_hash,
        role,
        institution_id,
        created_at
    )
SELECT
    'Auditor (Prueba RBAC)',
    'auditor@siladocs.test',
    '$2b$10$NLq6DEnnUTBdRP5pKbR67.UVzQz5dSyNacntc9Jqm2k68VAV.Q72C',
    'Auditor',
    (
        SELECT
            institution_id
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
        LIMIT
            1
    ),
    NOW ()
WHERE
    EXISTS (
        SELECT
            1
        FROM
            institutions
        WHERE
            name = 'Universidad Peruana de Ciencias Aplicadas'
    )
    AND NOT EXISTS (
        SELECT
            1
        FROM
            users
        WHERE
            email = 'auditor@siladocs.test'
    );
