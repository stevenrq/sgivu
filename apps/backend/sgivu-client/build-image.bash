#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-client si está corriendo..."
docker stop sgivu-client || true

echo "Eliminando contenedor sgivu-client si existe..."
docker rm sgivu-client || true

echo "Eliminando imagen stevenrq/sgivu-client:v1 si existe..."
docker rmi stevenrq/sgivu-client:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-client:v1..."
docker build -t stevenrq/sgivu-client:v1 .

echo "Publicando imagen stevenrq/sgivu-client:v1..."
docker push stevenrq/sgivu-client:v1

echo "Imagen stevenrq/sgivu-client:v1 construida y publicada correctamente."
