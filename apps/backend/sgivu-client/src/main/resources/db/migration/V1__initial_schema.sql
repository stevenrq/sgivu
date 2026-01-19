-- Migración inicial: Estructura de tablas para el servicio sgivu-client

-- =============================================================================
-- TABLA: addresses
-- Direcciones asociadas a clientes
-- =============================================================================
CREATE TABLE IF NOT EXISTS addresses
(
    id         BIGSERIAL PRIMARY KEY,
    street     VARCHAR(100)             NOT NULL,
    number     VARCHAR(20)              NOT NULL,
    city       VARCHAR(50)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para búsqueda y filtrado por ciudad
CREATE INDEX IF NOT EXISTS idx_addresses_city ON addresses (city);

-- =============================================================================
-- TABLA: clients
-- Información base de los clientes (personas o empresas)
-- =============================================================================
CREATE TABLE IF NOT EXISTS clients
(
    id           BIGSERIAL PRIMARY KEY,
    address_id   BIGINT                   NOT NULL,
    phone_number BIGINT                   NOT NULL UNIQUE,
    email        VARCHAR(40)              NOT NULL UNIQUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled      BOOLEAN                  NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_clients_address_id FOREIGN KEY (address_id) REFERENCES addresses (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Índices para búsqueda y filtrado
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients (email);
CREATE INDEX IF NOT EXISTS idx_clients_phone_number ON clients (phone_number);
CREATE INDEX IF NOT EXISTS idx_clients_enabled ON clients (enabled);
CREATE INDEX IF NOT EXISTS idx_clients_address_id ON clients (address_id);

-- =============================================================================
-- TABLA: persons
-- Clientes que son personas naturales
-- Relación 1:1 con clients (client_id es PK y FK)
-- =============================================================================
CREATE TABLE IF NOT EXISTS persons
(
    client_id   BIGINT PRIMARY KEY,
    national_id BIGINT      NOT NULL UNIQUE,
    first_name  VARCHAR(30) NOT NULL,
    last_name   VARCHAR(30) NOT NULL,
    CONSTRAINT fk_persons_client_id FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Índices para búsqueda por cédula y nombre
CREATE INDEX IF NOT EXISTS idx_persons_national_id ON persons (national_id);
CREATE INDEX IF NOT EXISTS idx_persons_names ON persons (first_name, last_name);

-- =============================================================================
-- TABLA: companies
-- Clientes que son empresas
-- Relación 1:1 con clients (client_id es PK y FK)
-- =============================================================================
CREATE TABLE IF NOT EXISTS companies
(
    client_id    BIGINT PRIMARY KEY,
    tax_id       VARCHAR(10) NOT NULL UNIQUE,
    company_name VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT fk_companies_client_id FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Índices para búsqueda por NIT y nombre de empresa
CREATE INDEX IF NOT EXISTS idx_companies_tax_id ON companies (tax_id);
CREATE INDEX IF NOT EXISTS idx_companies_company_name ON companies (company_name);

