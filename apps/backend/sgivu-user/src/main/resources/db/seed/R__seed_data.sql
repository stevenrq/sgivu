-- Datos iniciales para desarrollo y pruebas
-- Usa ON CONFLICT DO NOTHING para ser idempotente y compatible con datos de producción migrados.

INSERT INTO addresses (id, street, number, city, created_at, updated_at)
VALUES (1, 'Carrera 22a', '23-60', 'Montería', NOW(), NOW()),
       (2, 'Main Street', '12-34', 'Anytown', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO persons (id, national_id, first_name, last_name, phone_number, email, created_at, updated_at, address_id)
VALUES (1, 1003395547, 'Steven', 'Ricardo Quiñones', 3207108160, 'stevenrq8@gmail.com', NOW(), NOW(), 1),
       (2, 9876543210, 'Jane', 'Smith', 9876543210, 'janesmith@gmail.com', NOW(), NOW(), 2)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (person_id, username, password, enabled, account_non_expired, account_non_locked,
                   credentials_non_expired)
VALUES (1, 'steven', '$2a$12$Leo18wuk2P7KndFa3X3AperisbsMVJQ5WQE5JEB0DxM7FJPpiQn5q', true, true, true, true),
       (2, 'janesmith', '$2a$12$dflehQK6T6867eN73Fv0m.bvpgNgJy2v88s9L.6QAd.skoDCxdP/e', true, true, true, true)
ON CONFLICT (person_id) DO NOTHING;

INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES (1, 'ADMIN',
        'Rol con acceso total al sistema. Permite gestionar usuarios, roles, configuraciones, y supervisar todas las operaciones sin restricciones. Corresponde al ''Administrador General'' del sistema.',
        NOW(), NOW()),
       (2, 'USER',
        'Rol base para usuarios estándar del sistema. Sus capacidades específicas son definidas por los permisos adicionales que se le asignen. Ideal para roles como Vendedor, Mecánico o Comprador, que tienen acceso limitado a ciertas funcionalidades.',
        NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at, updated_at)
VALUES (1, 'user:create', 'Permite crear nuevos usuarios en el sistema', NOW(), NOW()),
       (2, 'user:read', 'Permite visualizar la información de los usuarios registrados.', NOW(), NOW()),
       (3, 'user:update', 'Permite modificar la información de los usuarios existentes.', NOW(), NOW()),
       (4, 'user:delete', 'Permite eliminar usuarios del sistema.', NOW(), NOW()),
       (5, 'role:create', 'Permite crear nuevos roles en el sistema.', NOW(), NOW()),
       (6, 'role:read', 'Permite visualizar la información de los roles registrados.', NOW(), NOW()),
       (7, 'role:update', 'Permite modificar la información de los roles existentes.', NOW(), NOW()),
       (8, 'role:delete', 'Permite eliminar roles del sistema.', NOW(), NOW()),
       (9, 'permission:create', 'Permite crear nuevos permisos en el sistema.', NOW(), NOW()),
       (10, 'permission:read', 'Permite visualizar la información de los permisos registrados.', NOW(), NOW()),
       (11, 'permission:update', 'Permite modificar la información de los permisos existentes.', NOW(), NOW()),
       (12, 'permission:delete', 'Permite eliminar permisos del sistema.', NOW(), NOW()),
       (13, 'person:create', 'Permite crear nuevas personas en el sistema.', NOW(), NOW()),
       (14, 'person:read', 'Permite visualizar la información de las personas registradas.', NOW(), NOW()),
       (15, 'person:update', 'Permite modificar la información de las personas existentes.', NOW(), NOW()),
       (16, 'person:delete', 'Permite eliminar personas del sistema.', NOW(), NOW()),
       (17, 'company:create', 'Permite crear nuevas empresas en el sistema.', NOW(), NOW()),
       (18, 'company:read', 'Permite visualizar la información de las empresas registradas.', NOW(), NOW()),
       (19, 'company:update', 'Permite modificar la información de las empresas existentes.', NOW(), NOW()),
       (20, 'company:delete', 'Permite eliminar empresas del sistema.', NOW(), NOW()),
       (21, 'vehicle:create', 'Permite crear nuevos vehículos en el sistema.', NOW(), NOW()),
       (22, 'vehicle:read', 'Permite visualizar la información de los vehículos registrados.', NOW(), NOW()),
       (23, 'vehicle:update', 'Permite modificar la información de los vehículos existentes.', NOW(), NOW()),
       (24, 'vehicle:delete', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       (25, 'car:create', 'Permite crear nuevos vehículos en el sistema.', NOW(), NOW()),
       (26, 'car:read', 'Permite visualizar la información de los vehículos registrados.', NOW(), NOW()),
       (27, 'car:update', 'Permite modificar la información de los vehículos existentes.', NOW(), NOW()),
       (28, 'car:delete', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       (29, 'motorcycle:create', 'Permite eliminar vehículos del sistema.', NOW(), NOW()),
       (30, 'motorcycle:read', 'Permite visualizar la información de las motocicletas registradas.', NOW(), NOW()),
       (31, 'motorcycle:update', 'Permite modificar la información de las motocicletas existentes.', NOW(), NOW()),
       (32, 'motorcycle:delete', 'Permite eliminar motocicletas del sistema.', NOW(), NOW()),
       (33, 'purchase_sale:create', 'Permite crear nuevos contratos de compraventa en el sistema.', NOW(), NOW()),
       (34, 'purchase_sale:read', 'Permite visualizar la información de los contratos de compraventa registrados.',
        NOW(),
        NOW()),
       (35, 'purchase_sale:update', 'Permite modificar la información de los contratos de compraventa existentes.',
        NOW(),
        NOW()),
       (36, 'purchase_sale:delete', 'Permite eliminar contratos de compraventa del sistema.', NOW(), NOW()),
       (37, 'ml:predict', 'Permite acceder a las funcionalidades de predicción del sistema.', NOW(), NOW()),
       (38, 'ml:retrain', 'Permite acceder a las funcionalidades de entrenamiento del sistema.', NOW(), NOW()),
       (39, 'ml:models', 'Permite gestionar los modelos de machine learning del sistema.', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles_permissions (role_id, permission_id)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7),
       (1, 8),
       (1, 9),
       (1, 10),
       (1, 11),
       (1, 12),
       (1, 13),
       (1, 14),
       (1, 15),
       (1, 16),
       (1, 17),
       (1, 18),
       (1, 19),
       (1, 20),
       (1, 21),
       (1, 22),
       (1, 23),
       (1, 24),
       (1, 25),
       (1, 26),
       (1, 27),
       (1, 28),
       (1, 29),
       (1, 30),
       (1, 31),
       (1, 32),
       (1, 33),
       (1, 34),
       (1, 35),
       (1, 36),
       (1, 37),
       (1, 38),
       (1, 39),
       (2, 2)
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO users_roles (user_id, role_id)
VALUES (1, 1), -- steven: ADMIN
       (1, 2), -- steven: USER
       (2, 2)  -- janesmith: USER
ON CONFLICT (user_id, role_id) DO NOTHING;
