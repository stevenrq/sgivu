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

echo "Deteniendo contenedor sgivu-config si está corriendo..."
docker stop sgivu-config || true

echo "Eliminando contenedor sgivu-config si existe..."
docker rm sgivu-config || true

echo "Eliminando imagen stevenrq/sgivu-config:latest si existe..."
docker rmi stevenrq/sgivu-config:latest || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-config:latest..."
docker build -t stevenrq/sgivu-config:latest .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-config:latest..."
	docker push stevenrq/sgivu-config:latest
	echo "Imagen stevenrq/sgivu-config:latest construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-config:latest construida localmente (no se hizo docker push)."
fi
