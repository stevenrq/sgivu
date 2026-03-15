#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-gateway si está corriendo..."
docker stop sgivu-gateway || true

echo "Eliminando contenedor sgivu-gateway si existe..."
docker rm sgivu-gateway || true

echo "Eliminando imagen stevenrq/sgivu-gateway:v1 si existe..."
docker rmi stevenrq/sgivu-gateway:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-gateway:v1..."
docker build -t stevenrq/sgivu-gateway:v1 .

echo "Publicando imagen stevenrq/sgivu-gateway:v1..."
docker push stevenrq/sgivu-gateway:v1

echo "Imagen stevenrq/sgivu-gateway:v1 construida y publicada correctamente."
