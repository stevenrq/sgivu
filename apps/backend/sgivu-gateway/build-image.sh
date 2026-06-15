#!/bin/bash
set -e

PUSH="false"

usage() {
	cat <<'EOF' >&2
Usage: $0 [--push]

Opciones:
  --push    Publica la imagen a Docker Hub después de construirla
  --help    Muestra esta ayuda
EOF
	exit 1
}

while [[ $# -gt 0 ]]; do
	case "$1" in
	--push)
		PUSH="true"
		shift
		;;
	-h | --help)
		usage
		;;
	*)
		echo "Argumento desconocido: $1" >&2
		usage
		;;
	esac
done

echo "Deteniendo contenedor sgivu-gateway si está corriendo..."
docker stop sgivu-gateway || true

echo "Eliminando contenedor sgivu-gateway si existe..."
docker rm sgivu-gateway || true

echo "Eliminando imagen stevenrq/sgivu-gateway:latest si existe..."
docker rmi stevenrq/sgivu-gateway:latest || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-gateway:latest..."
docker build -t stevenrq/sgivu-gateway:latest .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-gateway:latest..."
	docker push stevenrq/sgivu-gateway:latest
	echo "Imagen stevenrq/sgivu-gateway:latest construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-gateway:latest construida localmente (no se hizo docker push)."
fi
