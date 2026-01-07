DROP TABLE IF EXISTS addresses CASCADE;

DROP TABLE IF EXISTS clients CASCADE;

DROP TABLE IF EXISTS persons CASCADE;

DROP TABLE IF EXISTS companies CASCADE;

CREATE TABLE addresses
(
    id         BIGSERIAL PRIMARY KEY,
    street     VARCHAR(100)                                       NOT NULL,
    number     VARCHAR(20)                                        NOT NULL,
    city       VARCHAR(50)                                        NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE addresses
    OWNER TO postgres;

CREATE TABLE clients
(
    id           BIGSERIAL PRIMARY KEY,
    address_id   BIGINT                                             NOT NULL
        CONSTRAINT fk_clients_address_id REFERENCES addresses (id),
    phone_number BIGINT                                             NOT NULL UNIQUE,
    email        VARCHAR(40)                                        NOT NULL UNIQUE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    enabled      BOOLEAN                  DEFAULT TRUE              NOT NULL
);

ALTER TABLE clients
    OWNER TO postgres;

CREATE TABLE persons
(
    client_id   BIGINT PRIMARY KEY
        CONSTRAINT fk_persons_client_id REFERENCES clients (id),
    national_id BIGINT      NOT NULL UNIQUE,
    first_name  VARCHAR(30) NOT NULL,
    last_name   VARCHAR(30) NOT NULL
);

ALTER TABLE persons
    OWNER TO postgres;

CREATE TABLE companies
(
    client_id    BIGINT PRIMARY KEY
        CONSTRAINT fk_companies_client_id REFERENCES clients (id),
    tax_id       VARCHAR(10) NOT NULL UNIQUE,
    company_name VARCHAR(30) NOT NULL UNIQUE
);

ALTER TABLE companies
    OWNER TO postgres;
