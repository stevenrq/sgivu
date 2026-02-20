#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-vehicle si está corriendo..."
docker stop sgivu-vehicle || true

echo "Eliminando contenedor sgivu-vehicle si existe..."
docker rm sgivu-vehicle || true

echo "Eliminando imagen stevenrq/sgivu-vehicle:v1 si existe..."
docker rmi stevenrq/sgivu-vehicle:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-vehicle:v1..."
docker build -t stevenrq/sgivu-vehicle:v1 .

echo "Publicando imagen stevenrq/sgivu-vehicle:v1..."
docker push stevenrq/sgivu-vehicle:v1

echo "Imagen stevenrq/sgivu-vehicle:v1 construida y publicada correctamente."
