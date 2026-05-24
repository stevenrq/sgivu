-- Datos de prueba para desarrollo (responsables adicionales para pruebas E2E).
-- Steven y los datos base (roles, permisos) son creados por V2__seed_admin.sql.
-- Convención de ids cross-service (sin FK físico entre DBs):
--   users:     1..16   (1=steven, 2..16=responsables)
--   clients:   1..350  (persons 1..300, companies 301..350)
--   vehicles:  1..1500 (1..60 reservados Yamaha MT-03 línea 'MT')
--   contracts: 1..2500 (1..1500 PURCHASE, 1501..2500 SALE)
-- Idempotente: borra el rango 2..16 antes de re-insertar.

DELETE FROM users_roles WHERE user_id BETWEEN 2 AND 16;
DELETE FROM users WHERE person_id BETWEEN 2 AND 16;
DELETE FROM persons WHERE id BETWEEN 2 AND 16;
DELETE FROM addresses WHERE id BETWEEN 2 AND 16;

INSERT INTO addresses (id, street, number, city, created_at, updated_at)
SELECT
    i,
    'Calle ' || (90 + i),
    (10 + i) || '-' || (20 + i),
    'Montería',
    NOW(),
    NOW()
FROM generate_series(2, 16) AS i;

INSERT INTO persons (id, national_id, first_name, last_name, phone_number, email, created_at, updated_at, address_id)
SELECT
    i,
    1000000000 + i,
    (ARRAY['Javier','David','Luis','Daniel','Andrea','Pedro','Laura','Felipe','Sara','Manuel','Carmen','Veronica','Oscar','Paula','Camila'])[i - 1],
    (ARRAY['Perez','Ramirez','Gonzalez','Flores','Camacho','Cardenas','Silva','Andrade','Rios','Rojas','Ruiz','Duarte','Castillo','Mendez','Torres'])[i - 1],
    3001000000 + i,
    'responsable' || i || '@sgivu.local',
    NOW(),
    NOW(),
    i
FROM generate_series(2, 16) AS i;

-- Mismo hash bcrypt que Steven → login dev con la contraseña conocida.
INSERT INTO users (person_id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
SELECT i, 'responsable' || i, '$2a$12$WHvnGtNA.o5xAN7Oj3UFyOfB7MR7Yj.al8OXniAcmFMaHXqnr4Og2', true, true, true, true
FROM generate_series(2, 16) AS i;

-- Todos los usuarios 2..16 reciben USER (role_id 2) por defecto.
-- Algunos usuarios también reciben ADMIN (role_id 1).
INSERT INTO users_roles (user_id, role_id)
SELECT i, 2
FROM generate_series(2, 16) AS i
UNION ALL
SELECT i, 1
FROM generate_series(2, 9) AS i;

-- Sincronizar secuencias para evitar conflictos al insertar nuevos registros.
SELECT setval('addresses_id_seq', (SELECT MAX(id) FROM addresses));
SELECT setval('persons_id_seq',   (SELECT MAX(id) FROM persons));
