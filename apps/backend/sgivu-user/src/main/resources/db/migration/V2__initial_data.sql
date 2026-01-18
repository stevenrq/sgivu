-- Migración: Datos iniciales para desarrollo y pruebas

-- =============================================================================
-- DATOS: addresses
-- =============================================================================
INSERT INTO addresses (street, number, city, created_at, updated_at)
VALUES ('Calle 20', '5-15', 'Montería', NOW(), NOW()),
       ('Main Street', '123', 'Anytown', NOW(), NOW()),
       ('Avenida Siempre Viva', '742', 'Springfield', NOW(), NOW()),
       ('Carrera 50', '25-30', 'Bogotá', NOW(), NOW()),
       ('Calle de la Luna', '10', 'Medellín', NOW(), NOW()),
       ('Boulevard des Roses', '45', 'París', NOW(), NOW()),
       ('Plaza de España', '1', 'Madrid', NOW(), NOW()),
       ('Ocean Drive', '800', 'Miami', NOW(), NOW()),
       ('Via del Corso', '25', 'Roma', NOW(), NOW()),
       ('Rua do Ouro', '150', 'Lisboa', NOW(), NOW()),
       ('Sunset Boulevard', '101', 'Los Ángeles', NOW(), NOW());

-- =============================================================================
-- DATOS: persons
-- =============================================================================
INSERT INTO persons (national_id, first_name, last_name, phone_number, email, created_at, updated_at, address_id)
VALUES (1003395547, 'Steven', 'Ricardo Quiñones', 3207108160, 'stevenrq8@gmail.com', NOW(), NOW(), 1),
       (9876543210, 'Jane', 'Smith', 9876543210, 'janesmith@gmail.com', NOW(), NOW(), 2),
       (1112223303, 'Carlos', 'Lopez', 3001234503, 'carlos.lopez@example.com', NOW(), NOW(), 3),
       (1112223304, 'Ana', 'Martinez', 3001234504, 'ana.martinez@example.com', NOW(), NOW(), 4),
       (1112223305, 'Luis', 'Gonzalez', 3001234505, 'luis.gonzalez@example.com', NOW(), NOW(), 5),
       (1112223306, 'Maria', 'Rodriguez', 3001234506, 'maria.rodriguez@example.com', NOW(), NOW(), 6),
       (1112223307, 'Javier', 'Perez', 3001234507, 'javier.perez@example.com', NOW(), NOW(), 7),
       (1112223308, 'Laura', 'Sanchez', 3001234508, 'laura.sanchez@example.com', NOW(), NOW(), 8),
       (1112223309, 'David', 'Ramirez', 3001234509, 'david.ramirez@example.com', NOW(), NOW(), 9),
       (1112223310, 'Sofia', 'Torres', 3001234510, 'sofia.torres@example.com', NOW(), NOW(), 10),
       (1112223311, 'Daniel', 'Flores', 3001234511, 'daniel.flores@example.com', NOW(), NOW(), 11);

-- =============================================================================
-- DATOS: users
-- Contraseñas hasheadas con BCrypt (12 rondas)
-- steven: Steven123# | janesmith: Jane123# | los demás: Password123#
-- =============================================================================
INSERT INTO users (person_id, username, password, enabled, account_non_expired, account_non_locked,
                   credentials_non_expired)
VALUES (1, 'steven', '$2a$12$IDd2OgT.udehJK9550mpaO9YYcjU8b/Vw2QPhwAWcVFMZjej7gwq2', true, true, true, true),
       (2, 'janesmith', '$2a$12$dflehQK6T6867eN73Fv0m.bvpgNgJy2v88s9L.6QAd.skoDCxdP/e', true, true, true, true),
       (3, 'carloslopez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (4, 'anamartinez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (5, 'luisgonzalez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (6, 'mariarodriguez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (7, 'javierperez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (8, 'laurasanchez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (9, 'davidramirez', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (10, 'sofiatorres', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true),
       (11, 'danielflores', '$2a$12$egchYmbw../qBk/fGwKuzOiXi.W6/sTbCfqMeK9eYgTtys9k2j2D6', true, true, true, true);

-- =============================================================================
-- DATOS: roles
-- =============================================================================
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('ADMIN',
        'Rol con acceso total al sistema. Permite gestionar usuarios, roles, configuraciones, y supervisar todas las operaciones sin restricciones. Corresponde al ''Administrador General'' del sistema.',
        NOW(), NOW()),
       ('USER',
        'Rol base para usuarios estándar del sistema. Sus capacidades específicas son definidas por los permisos adicionales que se le asignen. Ideal para roles como Vendedor, Mecánico o Comprador, que tienen acceso limitado a ciertas funcionalidades.',
        NOW(), NOW());

-- =============================================================================
-- DATOS: permissions
-- Permisos CRUD por entidad del sistema
-- =============================================================================
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES ('user:create', 'Permite crear nuevos usuarios en el sistema', NOW(), NOW()),
       ('user:read', 'Permite visualizar la información de los usuarios registrados.', NOW(), NOW()),
       ('user:update', 'Permite modificar la información de los usuarios existentes.', NOW(), NOW()),
       ('user:delete', 'Permite eliminar usuarios del sistema.', NOW(), NOW()),
       ('role:create', 'Permite crear nuevos roles en el sistema.', NOW(), NOW()),
       ('role:read', 'Permite visualizar la información de los roles registrados.', NOW(), NOW()),
       ('role:update', 'Permite modificar la información de los roles existentes.', NOW(), NOW()),
       ('role:delete', 'Permite eliminar roles del sistema.', NOW(), NOW()),
       ('permission:create', 'Permite crear nuevos permisos en el sistema.', NOW(), NOW()),
       ('permission:read', 'Permite visualizar la información de los permisos registrados.', NOW(), NOW()),
       ('permission:update', 'Permite modificar la información de los permisos existentes.', NOW(), NOW()),
       ('permission:delete', 'Permite eliminar permisos del sistema.', NOW(), NOW()),
       ('person:create', 'Permite crear nuevas personas en el sistema.', NOW(), NOW()),
       ('person:read', 'Permite visualizar la información de las personas registradas.', NOW(), NOW()),
       ('person:update', 'Permite modificar la información de las personas existentes.', NOW(), NOW()),
       ('person:delete', 'Permite eliminar personas del sistema.', NOW(), NOW()),
       ('company:create', 'Permite crear nuevas empresas en el sistema.', NOW(), NOW()),
       ('company:read', 'Permite visualizar la información de las empresas registradas.', NOW(), NOW()),
       ('company:update', 'Permite modificar la información de las empresas existentes.', NOW(), NOW()),
       ('company:delete', 'Permite eliminar empresas del sistema.', NOW(), NOW()),
       ('vehicle:create', 'Permite crear nuevos vehículos en el sistema.', NOW(), NOW()),
       ('vehicle:read', 'Permite visualizar la información de los vehículos registrados.', NOW(), NOW()),
       ('vehicle:update', 'Permite modificar la información de los vehículos existentes.', NOW(), NOW()),
       ('vehicle:delete', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       ('car:create', 'Permite crear nuevos vehículos en el sistema.', NOW(), NOW()),
       ('car:read', 'Permite visualizar la información de los vehículos registrados.', NOW(), NOW()),
       ('car:update', 'Permite modificar la información de los vehículos existentes.', NOW(), NOW()),
       ('car:delete', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       ('motorcycle:create', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       ('motorcycle:read', 'Permite visualizar la información de las motocicletas registradas.', NOW(), NOW()),
       ('motorcycle:update', 'Permite modificar la información de las motocicletas existentes.', NOW(), NOW()),
       ('motorcycle:delete', 'Permite eliminar motocicletas del sistema.', NOW(), NOW()),
       ('purchase_sale:create', 'Permite crear nuevos contratos de compraventa en el sistema.', NOW(), NOW()),
       ('purchase_sale:read', 'Permite visualizar la información de los contratos de compraventa registrados.', NOW(), NOW()),
       ('purchase_sale:update', 'Permite modificar la información de los contratos de compraventa existentes.', NOW(), NOW()),
       ('purchase_sale:delete', 'Permite eliminar contratos de compraventa del sistema.', NOW(), NOW()),
       ('ml:predict', 'Permite acceder a las funcionalidades de predicción del sistema.', NOW(), NOW()),
       ('ml:retrain', 'Permite acceder a las funcionalidades de entrenamiento del sistema.', NOW(), NOW()),
       ('ml:models', 'Permite gestionar los modelos de machine learning del sistema.', NOW(), NOW());

-- =============================================================================
-- DATOS: roles_permissions
-- Asignación de permisos a roles (ADMIN tiene todos, USER solo lectura básica)
-- =============================================================================
INSERT INTO roles_permissions (role_id, permission_id)
VALUES (1, 1),  -- ADMIN: user:create
       (1, 2),  -- ADMIN: user:read
       (1, 3),  -- ADMIN: user:update
       (1, 4),  -- ADMIN: user:delete
       (1, 5),  -- ADMIN: role:create
       (1, 6),  -- ADMIN: role:read
       (1, 7),  -- ADMIN: role:update
       (1, 8),  -- ADMIN: role:delete
       (1, 9),  -- ADMIN: permission:create
       (1, 10), -- ADMIN: permission:read
       (1, 11), -- ADMIN: permission:update
       (1, 12), -- ADMIN: permission:delete
       (1, 13), -- ADMIN: person:create
       (1, 14), -- ADMIN: person:read
       (1, 15), -- ADMIN: person:update
       (1, 16), -- ADMIN: person:delete
       (1, 17), -- ADMIN: company:create
       (1, 18), -- ADMIN: company:read
       (1, 19), -- ADMIN: company:update
       (1, 20), -- ADMIN: company:delete
       (1, 21), -- ADMIN: vehicle:create
       (1, 22), -- ADMIN: vehicle:read
       (1, 23), -- ADMIN: vehicle:update
       (1, 24), -- ADMIN: vehicle:delete
       (1, 25), -- ADMIN: car:create
       (1, 26), -- ADMIN: car:read
       (1, 27), -- ADMIN: car:update
       (1, 28), -- ADMIN: car:delete
       (1, 29), -- ADMIN: motorcycle:create
       (1, 30), -- ADMIN: motorcycle:read
       (1, 31), -- ADMIN: motorcycle:update
       (1, 32), -- ADMIN: motorcycle:delete
       (1, 33), -- ADMIN: purchase_sale:create
       (1, 34), -- ADMIN: purchase_sale:read
       (1, 35), -- ADMIN: purchase_sale:update
       (1, 36), -- ADMIN: purchase_sale:delete
       (1, 37), -- ADMIN: ml:predict
       (1, 38), -- ADMIN: ml:retrain
       (1, 39), -- ADMIN: ml:models
       (2, 2);  -- USER: user:read

-- =============================================================================
-- DATOS: users_roles
-- Asignación de roles a usuarios
-- =============================================================================
INSERT INTO users_roles (user_id, role_id)
VALUES (1, 1),  -- steven: ADMIN
       (1, 2),  -- steven: USER
       (2, 2),  -- janesmith: USER
       (3, 2),  -- carloslopez: USER
       (4, 2),  -- anamartinez: USER
       (5, 2),  -- luisgonzalez: USER
       (6, 2),  -- mariarodriguez: USER
       (7, 2),  -- javierperez: USER
       (8, 2),  -- laurasanchez: USER
       (9, 2),  -- davidramirez: USER
       (10, 2), -- sofiatorres: USER
       (11, 2); -- danielflores: USER
