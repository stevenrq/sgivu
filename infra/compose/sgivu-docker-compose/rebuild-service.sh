#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'EOF' >&2
Uso: ./rebuild-service.sh --dev|--prod <servicio1> [<servicio2> ...] [--with-deps] [--pull] [--push] [--skip-build]

Opciones:
  --dev, -d       Usa docker-compose.dev.yml y .env.dev
  --prod, -p      Usa docker-compose.yml y .env
  --with-deps     Levanta dependencias directas del servicio
  --pull          Fuerza pull de la imagen antes de recrear el contenedor
  --push          Publica la imagen en Docker Hub después de construirla
  --skip-build    No ejecuta build/push, solo recrea el contenedor
EOF
	exit 1
}

MODE=""
SERVICES_TO_PROCESS=()
WITH_DEPS="false"
PULL="false"
PUSH="false"
SKIP_BUILD="false"

while [[ $# -gt 0 ]]; do
	case "$1" in
	--dev | -d)
		MODE="dev"
		shift
		;;
	--prod | -p)
		MODE="prod"
		shift
		;;
	--with-deps)
		WITH_DEPS="true"
		shift
		;;
	--pull)
		PULL="true"
		shift
		;;
	--push)
		PUSH="true"
		shift
		;;
	--skip-build)
		SKIP_BUILD="true"
		shift
		;;
	-h | --help)
		usage
		;;
	*)
		SERVICES_TO_PROCESS+=("$1")
		shift
		;;
	esac
done

if [[ -z "$MODE" || ${#SERVICES_TO_PROCESS[@]} -eq 0 ]]; then
	usage
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

SERVICES=(
	sgivu-auth
	sgivu-client
	sgivu-config
	sgivu-discovery
	sgivu-gateway
	sgivu-user
	sgivu-vehicle
	sgivu-purchase-sale
	sgivu-ml
)

declare -A SERVICE_DIRS=(
	["sgivu-auth"]="apps/backend/sgivu-auth"
	["sgivu-client"]="apps/backend/sgivu-client"
	["sgivu-config"]="apps/backend/sgivu-config"
	["sgivu-discovery"]="apps/backend/sgivu-discovery"
	["sgivu-gateway"]="apps/backend/sgivu-gateway"
	["sgivu-user"]="apps/backend/sgivu-user"
	["sgivu-vehicle"]="apps/backend/sgivu-vehicle"
	["sgivu-purchase-sale"]="apps/backend/sgivu-purchase-sale"
	["sgivu-ml"]="apps/ml/sgivu-ml"
)

declare -A SERVICE_TAGS=(
	["sgivu-auth"]="0.1.0"
	["sgivu-client"]="0.1.0"
	["sgivu-config"]="0.1.0"
	["sgivu-discovery"]="0.1.0"
	["sgivu-gateway"]="0.1.0"
	["sgivu-user"]="0.1.0"
	["sgivu-vehicle"]="0.1.0"
	["sgivu-purchase-sale"]="0.1.0"
	["sgivu-ml"]="0.1.0"
)

if [[ "$SKIP_BUILD" != "true" ]]; then
	for SERVICE in "${SERVICES_TO_PROCESS[@]}"; do
		REL_DIR="${SERVICE_DIRS[$SERVICE]:-}"
		if [[ -z "$REL_DIR" ]]; then
			echo "Servicio no soportado para build: $SERVICE" >&2
			echo "Servicios con build: ${SERVICES[*]}" >&2
			exit 1
		fi

		SERVICE_DIR="$ROOT_DIR/$REL_DIR"
		if [[ ! -d "$SERVICE_DIR" ]]; then
			echo "Directorio no existe: $SERVICE_DIR" >&2
			exit 1
		fi

		echo "=============================="
		echo "Construyendo $SERVICE..."
		echo "Directorio: $SERVICE_DIR"

		pushd "$SERVICE_DIR" >/dev/null

		if [[ -x "build-image.sh" ]]; then
			if [[ "$PUSH" == "true" ]]; then
				./build-image.sh --push
			else
				./build-image.sh
			fi
		else
			TAG="${SERVICE_TAGS[$SERVICE]:-latest}"
			if [[ -f "requirements.txt" ]]; then
				echo "Proyecto Python detectado, saltando Maven..."
			else
				./mvnw clean package -DskipTests
			fi

			docker build -t "stevenrq/$SERVICE:$TAG" .
			if [[ "$PUSH" == "true" ]]; then
				docker push "stevenrq/$SERVICE:$TAG"
			else
				echo "Imagen stevenrq/$SERVICE:$TAG construida localmente (no se hizo docker push)."
			fi
		fi

		popd >/dev/null
		echo "Build finalizado para $SERVICE."
	done
	echo "Build finalizado para todos los servicios solicitados."
fi

COMPOSE_DIR="$ROOT_DIR/infra/compose/sgivu-docker-compose"

if [[ "$MODE" == "dev" ]]; then
	COMPOSE_FILE="docker-compose.dev.yml"
	ENV_ARGS=(--env-file .env.dev)
else
	COMPOSE_FILE="docker-compose.yml"
	ENV_ARGS=()
fi

pushd "$COMPOSE_DIR" >/dev/null
# Verificar que todos los servicios solicitados existan en el compose
AVAILABLE_SERVICES=$(docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" config --services)
for SERVICE in "${SERVICES_TO_PROCESS[@]}"; do
	if ! echo "$AVAILABLE_SERVICES" | grep -qx "$SERVICE"; then
		echo "Servicio no definido en $COMPOSE_FILE: $SERVICE" >&2
		popd >/dev/null
		exit 1
	fi
done

if [[ "$PULL" == "true" ]]; then
	docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" pull "${SERVICES_TO_PROCESS[@]}"
fi

NO_DEPS_ARGS=()
if [[ "$WITH_DEPS" != "true" ]]; then
	NO_DEPS_ARGS=(--no-deps)
fi

docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" up -d "${NO_DEPS_ARGS[@]}" --force-recreate "${SERVICES_TO_PROCESS[@]}"

popd >/dev/null

echo "Contenedores ${SERVICES_TO_PROCESS[*]} recreados en Compose."
