#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-purchase-sale si está corriendo..."
docker stop sgivu-purchase-sale || true

echo "Eliminando contenedor sgivu-purchase-sale si existe..."
docker rm sgivu-purchase-sale || true

echo "Eliminando imagen stevenrq/sgivu-purchase-sale:v1 si existe..."
docker rmi stevenrq/sgivu-purchase-sale:v1 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-purchase-sale:v1..."
docker build -t stevenrq/sgivu-purchase-sale:v1 .

echo "Publicando imagen stevenrq/sgivu-purchase-sale:v1..."
docker push stevenrq/sgivu-purchase-sale:v1

echo "Imagen stevenrq/sgivu-purchase-sale:v1 construida y publicada correctamente."
