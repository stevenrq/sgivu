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

echo "Eliminando imagen stevenrq/sgivu-gateway:0.1.0 si existe..."
docker rmi stevenrq/sgivu-gateway:0.1.0 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-gateway:0.1.0..."
docker build -t stevenrq/sgivu-gateway:0.1.0 .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-gateway:0.1.0..."
	docker push stevenrq/sgivu-gateway:0.1.0
	echo "Imagen stevenrq/sgivu-gateway:0.1.0 construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-gateway:0.1.0 construida localmente (no se hizo docker push)."
fi
