-- Migración inicial: Estructura de tablas para el servicio sgivu-vehicle

-- =============================================================================
-- TABLA: vehicles
-- Tabla base para todos los vehículos del inventario de usados
-- Centraliza atributos comunes (placas, números de motor/chasis, etc.)
-- =============================================================================
CREATE TABLE IF NOT EXISTS vehicles
(
    id              BIGSERIAL PRIMARY KEY,
    brand           VARCHAR(20)              NOT NULL,
    model           VARCHAR(20)              NOT NULL,
    capacity        INTEGER                  NOT NULL,
    line            VARCHAR(20)              NOT NULL,
    plate           VARCHAR(10)              NOT NULL UNIQUE,
    motor_number    VARCHAR(30)              NOT NULL UNIQUE,
    serial_number   VARCHAR(30)              NOT NULL UNIQUE,
    chassis_number  VARCHAR(30)              NOT NULL UNIQUE,
    color           VARCHAR(20)              NOT NULL,
    city_registered VARCHAR(30)              NOT NULL,
    year            INTEGER                  NOT NULL CHECK (year >= 1950 AND year <= 2050),
    mileage         INTEGER                  NOT NULL CHECK (mileage >= 0),
    transmission    VARCHAR(20)              NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    purchase_price  DOUBLE PRECISION         NOT NULL CHECK (purchase_price >= 0),
    sale_price      DOUBLE PRECISION         NOT NULL CHECK (sale_price >= 0),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para búsqueda y filtrado eficiente de vehículos
CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles (status);
CREATE INDEX IF NOT EXISTS idx_vehicles_brand_model ON vehicles (brand, model);
CREATE INDEX IF NOT EXISTS idx_vehicles_year ON vehicles (year);
CREATE INDEX IF NOT EXISTS idx_vehicles_plate ON vehicles (plate);
CREATE INDEX IF NOT EXISTS idx_vehicles_city_registered ON vehicles (city_registered);

-- Secuencia para generación de IDs
CREATE SEQUENCE IF NOT EXISTS vehicles_id_seq START WITH 1 INCREMENT BY 1;

-- =============================================================================
-- TABLA: cars
-- Automóviles en el inventario (hereda de vehicles)
-- Añade tipología de carrocería, combustible y número de puertas
-- =============================================================================
CREATE TABLE IF NOT EXISTS cars
(
    vehicle_id      BIGINT PRIMARY KEY,
    body_type       VARCHAR(20) NOT NULL,
    fuel_type       VARCHAR(20) NOT NULL,
    number_of_doors INTEGER     NOT NULL,
    CONSTRAINT fk_cars_vehicles_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- Índices para búsqueda por tipo de carrocería y combustible
CREATE INDEX IF NOT EXISTS idx_cars_body_type ON cars (body_type);
CREATE INDEX IF NOT EXISTS idx_cars_fuel_type ON cars (fuel_type);

-- =============================================================================
-- TABLA: motorcycles
-- Motocicletas en el inventario (hereda de vehicles)
-- Añade tipo de motocicleta (scooter, deportiva, alto cilindraje, etc.)
-- =============================================================================
CREATE TABLE IF NOT EXISTS motorcycles
(
    vehicle_id      BIGINT PRIMARY KEY,
    motorcycle_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_motorcycles_vehicles_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- Índice para búsqueda por tipo de motocicleta
CREATE INDEX IF NOT EXISTS idx_motorcycles_motorcycle_type ON motorcycles (motorcycle_type);

-- =============================================================================
-- TABLA: vehicle_images
-- Metadatos de imágenes asociadas a vehículos (almacenadas en S3)
-- Permite auditoría de uploads y control de imagen principal
-- =============================================================================
CREATE TABLE IF NOT EXISTS vehicle_images
(
    id         BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT                   NOT NULL,
    bucket     VARCHAR(100)             NOT NULL,
    key        VARCHAR(255)             NOT NULL UNIQUE,
    file_name  VARCHAR(255)             NOT NULL UNIQUE,
    mime_type  VARCHAR(100)             NULL,
    file_size  BIGINT                   NULL,
    is_primary BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_images_vehicles_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- Índices para búsqueda de imágenes por vehículo e imagen principal
CREATE INDEX IF NOT EXISTS idx_vehicle_images_vehicle_id ON vehicle_images (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_images_is_primary ON vehicle_images (is_primary);

-- Secuencia para generación de IDs
CREATE SEQUENCE IF NOT EXISTS vehicle_images_id_seq START WITH 1 INCREMENT BY 1;
