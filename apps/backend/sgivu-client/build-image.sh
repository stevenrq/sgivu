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

echo "Deteniendo contenedor sgivu-client si está corriendo..."
docker stop sgivu-client || true

echo "Eliminando contenedor sgivu-client si existe..."
docker rm sgivu-client || true

echo "Eliminando imagen stevenrq/sgivu-client:latest si existe..."
docker rmi stevenrq/sgivu-client:latest || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-client:latest..."
docker build -t stevenrq/sgivu-client:latest .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-client:latest..."
	docker push stevenrq/sgivu-client:latest
	echo "Imagen stevenrq/sgivu-client:latest construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-client:latest construida localmente (no se hizo docker push)."
fi
