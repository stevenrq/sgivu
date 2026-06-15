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

echo "Deteniendo contenedor sgivu-ml si está corriendo..."
docker stop sgivu-ml || true

echo "Eliminando contenedor sgivu-ml si existe..."
docker rm sgivu-ml || true

echo "Eliminando imagen stevenrq/sgivu-ml:latest si existe..."
docker rmi stevenrq/sgivu-ml:latest || true

echo "Construyendo imagen Docker stevenrq/sgivu-ml:latest..."
docker buildx build --load -t stevenrq/sgivu-ml:latest .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-ml:latest..."
	docker push stevenrq/sgivu-ml:latest
	echo "Imagen stevenrq/sgivu-ml:latest construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-ml:latest construida localmente (no se hizo docker push)."
fi
