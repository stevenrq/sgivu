#!/bin/bash
set -e

echo "Deteniendo contenedor sgivu-ml si está corriendo..."
docker stop sgivu-ml || true

echo "Eliminando contenedor sgivu-ml si existe..."
docker rm sgivu-ml || true

echo "Eliminando imagen stevenrq/sgivu-ml:v1 si existe..."
docker rmi stevenrq/sgivu-ml:v1 || true

echo "Construyendo imagen Docker stevenrq/sgivu-ml:v1..."
docker build -t stevenrq/sgivu-ml:v1 .

echo "Publicando imagen stevenrq/sgivu-ml:v1..."
docker push stevenrq/sgivu-ml:v1

echo "Imagen stevenrq/sgivu-ml:v1 construida y publicada correctamente."
