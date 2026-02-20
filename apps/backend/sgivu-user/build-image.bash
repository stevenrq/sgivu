#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-user si está corriendo..."
docker stop sgivu-user || true

echo "Eliminando contenedor sgivu-user si existe..."
docker rm sgivu-user || true

echo "Eliminando imagen stevenrq/sgivu-user:v1 si existe..."
docker rmi stevenrq/sgivu-user:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-user:v1..."
docker build -t stevenrq/sgivu-user:v1 .

echo "Publicando imagen stevenrq/sgivu-user:v1..."
docker push stevenrq/sgivu-user:v1

echo "Imagen stevenrq/sgivu-user:v1 construida y publicada correctamente."
