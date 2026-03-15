#!/bin/bash
# Cambia el plugin de autenticación del usuario Zipkin a mysql_native_password.
# MySQL 8.4+ usa caching_sha2_password por defecto, pero el cliente MariaDB JDBC
# de Zipkin no lo soporta sin configuración RSA adicional.
# Este script se ejecuta antes de zipkin-user.sql (orden alfabético en docker-entrypoint-initdb.d).
# No debe tener permisos de ejecución (+x) para que el entrypoint lo 'source'
# y tenga acceso a la función docker_process_sql (que usa el socket correcto).

docker_process_sql --database=mysql <<-EOSQL
	ALTER USER '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
	FLUSH PRIVILEGES;
EOSQL
