#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-config si está corriendo..."
docker stop sgivu-config || true

echo "Eliminando contenedor sgivu-config si existe..."
docker rm sgivu-config || true

echo "Eliminando imagen stevenrq/sgivu-config:v1 si existe..."
docker rmi stevenrq/sgivu-config:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-config:v1..."
docker build -t stevenrq/sgivu-config:v1 .

echo "Publicando imagen stevenrq/sgivu-config:v1..."
docker push stevenrq/sgivu-config:v1

echo "Imagen stevenrq/sgivu-config:v1 construida y publicada correctamente."
