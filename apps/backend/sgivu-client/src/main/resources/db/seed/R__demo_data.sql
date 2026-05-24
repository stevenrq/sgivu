-- Demo data — development environment only
-- 350 clientes = 300 personas (ids 1..300) + 50 empresas (ids 301..350).
-- Convención de ids cross-service (sin FK físico entre DBs):
--   clients:   1..350  (persons 1..300, companies 301..350)
--   users:     1..16   (1=steven, 2..16=responsables)
--   vehicles:  1..1500 (1..60 reservados Yamaha MT-03 línea 'MT')
--   contracts: 1..2500 (1..1500 PURCHASE, 1501..2500 SALE)
--
-- Idempotente: TRUNCATE ... RESTART IDENTITY CASCADE antes de re-insertar.
-- Cualquier edición del archivo dispara un checksum nuevo → Flyway re-ejecuta.

DO
$$
    BEGIN
        PERFORM setseed(0.42);

        TRUNCATE addresses, clients, persons, companies RESTART IDENTITY CASCADE;

        -- ============================================================
        -- addresses: una dirección por cliente (ids 1..350)
        -- ============================================================
        INSERT INTO addresses (id, street, number, city)
        SELECT i,
               'Calle ' || (10 + (i % 200)),
               (1 + (i % 150))::TEXT || '-' || (1 + ((i * 7) % 99))::TEXT,
               (ARRAY ['Medellín','Bogotá','Cali','Barranquilla','Bucaramanga','Pereira','Cartagena','Manizales'])[1 + ((i - 1) % 8)]
        FROM generate_series(1, 350) AS i;

        -- ============================================================
        -- clients: 1..350 (1:1 con addresses)
        -- ============================================================
        INSERT INTO clients (id, address_id, phone_number, email, enabled)
        SELECT i,
               i,
               3000000000 + i,
               'client' || i || '@demo.sgivu.local',
               TRUE
        FROM generate_series(1, 350) AS i;

        -- ============================================================
        -- persons: 1..300 (ids compartidos con clients.id)
        -- ============================================================
        INSERT INTO persons (client_id, national_id, first_name, last_name)
        SELECT i,
               1010000000 + i,
               (ARRAY ['María','Carlos','Laura','Ana','Andrés','Camila','Felipe','Sofía','Mateo','Daniel','Valentina','Javier','Luis','Isabella','Juan'])[1 + ((i - 1) % 15)],
               (ARRAY ['García','Rodríguez','Martínez','Pérez','Gómez','Ramírez','Flores','Castro','Torres','Ruiz','Salazar','Fernández','Vargas','Silva','Morales'])[1 + ((i * 7) % 15)]
        FROM generate_series(1, 300) AS i;

        -- ============================================================
        -- companies: 301..350 (ids compartidos con clients.id)
        -- tax_id '9' + 9 dígitos (10 chars total, cabe VARCHAR(10))
        -- ============================================================
        INSERT INTO companies (client_id, tax_id, company_name)
        SELECT i,
               '9' || lpad((i)::TEXT, 9, '0'),
               'Empresa Demo ' || (i - 300)
        FROM generate_series(301, 350) AS i;

        -- Sincronizar secuencias
        PERFORM setval('addresses_id_seq', 350);
        PERFORM setval('clients_id_seq', 350);
    END
$$;
