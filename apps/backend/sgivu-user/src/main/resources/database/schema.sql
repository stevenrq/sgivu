DROP TABLE IF EXISTS permissions CASCADE;

DROP TABLE IF EXISTS users_roles CASCADE;

DROP TABLE IF EXISTS roles_permissions CASCADE;

DROP TABLE IF EXISTS users CASCADE;

DROP TABLE IF EXISTS roles CASCADE;

DROP TABLE IF EXISTS persons CASCADE;

DROP TABLE IF EXISTS addresses CASCADE;

CREATE TABLE IF NOT EXISTS permissions
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(20)              NOT NULL UNIQUE,
    description TEXT                     NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS addresses
(
    id         BIGSERIAL PRIMARY KEY,
    street     VARCHAR(100)             NOT NULL,
    number     VARCHAR(20)              NOT NULL,
    city       VARCHAR(50)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS persons
(
    id           BIGSERIAL PRIMARY KEY,
    national_id  BIGINT                   NOT NULL UNIQUE,
    first_name   VARCHAR(20)              NOT NULL,
    last_name    VARCHAR(20)              NOT NULL,
    phone_number BIGINT                   NOT NULL UNIQUE,
    email        VARCHAR(40)              NOT NULL UNIQUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    address_id   BIGINT UNIQUE,
    CONSTRAINT fk_persons_addresses_id FOREIGN KEY (address_id) REFERENCES addresses (id) ON DELETE
        SET
        NULL
);

CREATE TABLE IF NOT EXISTS roles
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(20)              NOT NULL UNIQUE,
    description TEXT                     NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users
(
    person_id               BIGINT PRIMARY KEY,
    username                VARCHAR(20) NOT NULL UNIQUE,
    password                VARCHAR(60) NOT NULL,
    enabled                 BOOLEAN     NOT NULL DEFAULT TRUE,
    account_non_expired     BOOLEAN     NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN     NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_users_persons_id FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE IF NOT EXISTS roles_permissions
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_roles_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_roles_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE IF NOT EXISTS users_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user_id FOREIGN KEY (user_id) REFERENCES users (person_id),
    CONSTRAINT fk_users_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
