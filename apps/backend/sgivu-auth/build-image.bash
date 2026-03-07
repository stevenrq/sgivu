#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-auth si está corriendo..."
docker stop sgivu-auth || true

echo "Eliminando contenedor sgivu-auth si existe..."
docker rm sgivu-auth || true

echo "Eliminando imagen stevenrq/sgivu-auth:v1 si existe..."
docker rmi stevenrq/sgivu-auth:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-auth:v1..."
docker build -t stevenrq/sgivu-auth:v1 .

echo "Publicando imagen stevenrq/sgivu-auth:v1..."
docker push stevenrq/sgivu-auth:v1

echo "Imagen stevenrq/sgivu-auth:v1 construida y publicada correctamente."
