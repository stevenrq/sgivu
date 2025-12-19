drop sequence if exists public.vehicles_id_seq cascade;
drop table if exists public.cars cascade;
drop table if exists public.motorcycles cascade;
drop table if exists public.vehicles cascade;
drop table if exists public.vehicle_images cascade;

CREATE SEQUENCE vehicles_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE vehicle_images_id_seq START WITH 1 INCREMENT BY 1;


CREATE TABLE vehicles
(
    id              BIGINT PRIMARY KEY DEFAULT nextval('vehicles_id_seq'),
    brand           VARCHAR(20)      NOT NULL,
    model           VARCHAR(20)      NOT NULL,
    capacity        INT              NOT NULL,
    line            VARCHAR(20)      NOT NULL,
    plate           VARCHAR(10)      NOT NULL UNIQUE,
    motor_number    VARCHAR(30)      NOT NULL UNIQUE,
    serial_number   VARCHAR(30)      NOT NULL UNIQUE,
    chassis_number  VARCHAR(30)      NOT NULL UNIQUE,
    color           VARCHAR(20)      NOT NULL,
    city_registered VARCHAR(30)      NOT NULL,
    year            INT              NOT NULL CHECK (year BETWEEN 1950 AND 2050),
    mileage         INT              NOT NULL CHECK (mileage >= 0),
    transmission    VARCHAR(20)      NOT NULL,
    status          VARCHAR(20)      NOT NULL,
    purchase_price  DOUBLE PRECISION NOT NULL CHECK (purchase_price >= 0),
    sale_price      DOUBLE PRECISION NOT NULL CHECK (sale_price >= 0),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);


CREATE TABLE cars
(
    vehicle_id      BIGINT PRIMARY KEY,
    body_type       VARCHAR(20) NOT NULL,
    fuel_type       VARCHAR(20) NOT NULL,
    number_of_doors INT         NOT NULL,
    CONSTRAINT fk_car_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

CREATE TABLE motorcycles
(
    vehicle_id      BIGINT PRIMARY KEY,
    motorcycle_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_motorcycle_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

CREATE TABLE vehicle_images
(
    id         BIGINT PRIMARY KEY    DEFAULT nextval('vehicle_images_id_seq'),
    vehicle_id BIGINT       NOT NULL,
    bucket     VARCHAR(100) NOT NULL,

    key        VARCHAR(255) NOT NULL,
    file_name  VARCHAR(255) NOT NULL,

    mime_type  VARCHAR(100),
    file_size  BIGINT,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_vehicle_image_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
            ON DELETE CASCADE,

    CONSTRAINT vehicle_images_key_uk UNIQUE (key),
    CONSTRAINT vehicle_images_file_name_uk UNIQUE (file_name)
);
