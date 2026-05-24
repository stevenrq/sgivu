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

echo "Deteniendo contenedor sgivu-discovery si está corriendo..."
docker stop sgivu-discovery || true

echo "Eliminando contenedor sgivu-discovery si existe..."
docker rm sgivu-discovery || true

echo "Eliminando imagen stevenrq/sgivu-discovery:0.1.0 si existe..."
docker rmi stevenrq/sgivu-discovery:0.1.0 || true

echo "Construyendo artefacto con Maven..."
./mvnw clean package -DskipTests

echo "Construyendo imagen Docker stevenrq/sgivu-discovery:0.1.0..."
docker build -t stevenrq/sgivu-discovery:0.1.0 .

if [[ "$PUSH" == "true" ]]; then
	echo "Publicando imagen stevenrq/sgivu-discovery:0.1.0..."
	docker push stevenrq/sgivu-discovery:0.1.0
	echo "Imagen stevenrq/sgivu-discovery:0.1.0 construida y publicada correctamente."
else
	echo "Imagen stevenrq/sgivu-discovery:0.1.0 construida localmente (no se hizo docker push)."
fi
