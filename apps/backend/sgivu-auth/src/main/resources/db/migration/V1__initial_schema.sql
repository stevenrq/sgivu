-- Migración inicial: Estructura de tablas para el servicio sgivu-auth
-- OAuth2 Authorization Server + Spring Session

-- =============================================================================
-- TABLA: clients
-- Clientes OAuth2 registrados en el Authorization Server
-- =============================================================================
CREATE TABLE IF NOT EXISTS clients
(
    id                            VARCHAR(255)                            NOT NULL,
    client_id                     VARCHAR(255)                            NOT NULL,
    client_id_issued_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(255)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(255)                            NOT NULL,
    client_authentication_methods VARCHAR(1000)                           NOT NULL,
    authorization_grant_types     VARCHAR(1000)                           NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000)                           NOT NULL,
    client_settings               TEXT                                    NOT NULL,
    token_settings                TEXT                                    NOT NULL,
    PRIMARY KEY (id)
);

-- Índice para búsqueda por client_id (principal método de autenticación)
CREATE UNIQUE INDEX IF NOT EXISTS idx_clients_client_id ON clients (client_id);

-- =============================================================================
-- TABLA: authorizations
-- Autorizaciones OAuth2 activas (tokens, códigos de autorización, etc.)
-- =============================================================================
CREATE TABLE IF NOT EXISTS authorizations
(
    id                            VARCHAR(255) NOT NULL,
    registered_client_id          VARCHAR(255) NOT NULL,
    principal_name                VARCHAR(255) NOT NULL,
    authorization_grant_type      VARCHAR(255) NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    TEXT          DEFAULT NULL,
    state                         VARCHAR(500)  DEFAULT NULL,
    authorization_code_value      TEXT          DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMP     DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP     DEFAULT NULL,
    authorization_code_metadata   TEXT          DEFAULT NULL,
    access_token_value            TEXT          DEFAULT NULL,
    access_token_issued_at        TIMESTAMP     DEFAULT NULL,
    access_token_expires_at       TIMESTAMP     DEFAULT NULL,
    access_token_metadata         TEXT          DEFAULT NULL,
    access_token_type             VARCHAR(255)  DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    refresh_token_value           TEXT          DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMP     DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMP     DEFAULT NULL,
    refresh_token_metadata        TEXT          DEFAULT NULL,
    oidc_id_token_value           TEXT          DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMP     DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMP     DEFAULT NULL,
    oidc_id_token_metadata        TEXT          DEFAULT NULL,
    oidc_id_token_claims          TEXT          DEFAULT NULL,
    user_code_value               TEXT          DEFAULT NULL,
    user_code_issued_at           TIMESTAMP     DEFAULT NULL,
    user_code_expires_at          TIMESTAMP     DEFAULT NULL,
    user_code_metadata            TEXT          DEFAULT NULL,
    device_code_value             TEXT          DEFAULT NULL,
    device_code_issued_at         TIMESTAMP     DEFAULT NULL,
    device_code_expires_at        TIMESTAMP     DEFAULT NULL,
    device_code_metadata          TEXT          DEFAULT NULL,
    PRIMARY KEY (id)
);

-- Índices para búsqueda de autorizaciones activas
CREATE INDEX IF NOT EXISTS idx_authorizations_principal_name ON authorizations (principal_name);
CREATE INDEX IF NOT EXISTS idx_authorizations_client_id ON authorizations (registered_client_id);
CREATE INDEX IF NOT EXISTS idx_authorizations_state ON authorizations (state);
-- Índice para limpieza de tokens expirados
CREATE INDEX IF NOT EXISTS idx_authorizations_access_token_expires_at ON authorizations (access_token_expires_at);
CREATE INDEX IF NOT EXISTS idx_authorizations_refresh_token_expires_at ON authorizations (refresh_token_expires_at);

-- =============================================================================
-- TABLA: authorization_consents
-- Consentimientos de usuario para clientes OAuth2
-- =============================================================================
CREATE TABLE IF NOT EXISTS authorization_consents
(
    registered_client_id VARCHAR(255)  NOT NULL,
    principal_name       VARCHAR(255)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- =============================================================================
-- TABLA: SPRING_SESSION
-- Sesiones HTTP gestionadas por Spring Session JDBC
-- =============================================================================
CREATE TABLE IF NOT EXISTS SPRING_SESSION
(
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

-- =============================================================================
-- TABLA: SPRING_SESSION_ATTRIBUTES
-- Atributos serializados de las sesiones HTTP
-- =============================================================================
CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
