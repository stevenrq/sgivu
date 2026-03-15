#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-discovery si está corriendo..."
docker stop sgivu-discovery || true

echo "Eliminando contenedor sgivu-discovery si existe..."
docker rm sgivu-discovery || true

echo "Eliminando imagen stevenrq/sgivu-discovery:v1 si existe..."
docker rmi stevenrq/sgivu-discovery:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-discovery:v1..."
docker build -t stevenrq/sgivu-discovery:v1 .

echo "Publicando imagen stevenrq/sgivu-discovery:v1..."
docker push stevenrq/sgivu-discovery:v1

echo "Imagen stevenrq/sgivu-discovery:v1 construida y publicada correctamente."
