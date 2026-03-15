#!/bin/bash
# Crea las bases de datos de cada microservicio en la instancia compartida de PostgreSQL.
# Se ejecuta automáticamente en el primer inicio (volumen postgres-data vacío) gracias
# al montaje en /docker-entrypoint-initdb.d de la imagen postgres:16.
# Las tablas y datos semilla los gestiona Flyway (Spring Boot) o Alembic (sgivu-ml).

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE sgivu_auth_db;
    CREATE DATABASE sgivu_user_db;
    CREATE DATABASE sgivu_client_db;
    CREATE DATABASE sgivu_vehicle_db;
    CREATE DATABASE sgivu_purchase_sale_db;
    CREATE DATABASE sgivu_ml_db;
EOSQL
