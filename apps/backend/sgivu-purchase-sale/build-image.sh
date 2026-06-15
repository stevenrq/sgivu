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

echo "Deteniendo contenedor sgivu-purchase-sale si está corriendo..."
docker stop sgivu-purchase-sale || true

echo "Eliminando contenedor sgivu-purchase-sale si existe..."
docker rm sgivu-purchase-sale || true

echo "Eliminando imagen stevenrq/sgivu-purchase-sale:latest si existe..."
docker rmi stevenrq/sgivu-purchase-sale:latest || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-purchase-sale:latest..."
docker build -t stevenrq/sgivu-purchase-sale:latest .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-purchase-sale:latest..."
	docker push stevenrq/sgivu-purchase-sale:latest
	echo "Imagen stevenrq/sgivu-purchase-sale:latest construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-purchase-sale:latest construida localmente (no se hizo docker push)."
fi
