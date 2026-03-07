
CREATE TABLE IF NOT EXISTS addresses
(
    id         BIGSERIAL PRIMARY KEY,
    street     VARCHAR(100)             NOT NULL,
    number     VARCHAR(20)              NOT NULL,
    city       VARCHAR(50)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_addresses_city ON addresses (city);

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

CREATE INDEX IF NOT EXISTS idx_clients_email ON clients (email);
CREATE INDEX IF NOT EXISTS idx_clients_phone_number ON clients (phone_number);
CREATE INDEX IF NOT EXISTS idx_clients_enabled ON clients (enabled);
CREATE INDEX IF NOT EXISTS idx_clients_address_id ON clients (address_id);

CREATE TABLE IF NOT EXISTS persons
(
    client_id   BIGINT PRIMARY KEY,
    national_id BIGINT      NOT NULL UNIQUE,
    first_name  VARCHAR(30) NOT NULL,
    last_name   VARCHAR(30) NOT NULL,
    CONSTRAINT fk_persons_client_id FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_persons_national_id ON persons (national_id);
CREATE INDEX IF NOT EXISTS idx_persons_names ON persons (first_name, last_name);

CREATE TABLE IF NOT EXISTS companies
(
    client_id    BIGINT PRIMARY KEY,
    tax_id       VARCHAR(10) NOT NULL UNIQUE,
    company_name VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT fk_companies_client_id FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_companies_tax_id ON companies (tax_id);
CREATE INDEX IF NOT EXISTS idx_companies_company_name ON companies (company_name);

