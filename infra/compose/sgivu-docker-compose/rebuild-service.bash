#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF' >&2
Uso: ./rebuild-service.bash --dev|--prod <servicio> [--with-deps] [--pull] [--skip-build]

Opciones:
  --dev, -d       Usa docker-compose.dev.yml y .env.dev
  --prod, -p      Usa docker-compose.yml y .env
  --with-deps     Levanta dependencias directas del servicio
  --pull          Fuerza pull de la imagen antes de recrear el contenedor
  --skip-build    No ejecuta build/push, solo recrea el contenedor
EOF
  exit 1
}

MODE=""
SERVICE=""
WITH_DEPS="false"
PULL="false"
SKIP_BUILD="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dev|-d)
      MODE="dev"
      shift
      ;;
    --prod|-p)
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
    --skip-build)
      SKIP_BUILD="true"
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      if [[ -z "$SERVICE" ]]; then
        SERVICE="$1"
        shift
      else
        echo "Argumento desconocido: $1" >&2
        usage
      fi
      ;;
  esac
done

if [[ -z "$MODE" || -z "$SERVICE" ]]; then
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
  [sgivu-auth]="apps/backend/sgivu-auth"
  [sgivu-client]="apps/backend/sgivu-client"
  [sgivu-config]="apps/backend/sgivu-config"
  [sgivu-discovery]="apps/backend/sgivu-discovery"
  [sgivu-gateway]="apps/backend/sgivu-gateway"
  [sgivu-user]="apps/backend/sgivu-user"
  [sgivu-vehicle]="apps/backend/sgivu-vehicle"
  [sgivu-purchase-sale]="apps/backend/sgivu-purchase-sale"
  [sgivu-ml]="apps/ml/sgivu-ml"
)

declare -A SERVICE_TAGS=(
  [sgivu-auth]="v1"
  [sgivu-client]="v1"
  [sgivu-config]="v1"
  [sgivu-discovery]="v1"
  [sgivu-gateway]="v1"
  [sgivu-user]="v1"
  [sgivu-vehicle]="v1"
  [sgivu-purchase-sale]="v1"
  [sgivu-ml]="v1"
)

REL_DIR="${SERVICE_DIRS[$SERVICE]:-}"

if [[ "$SKIP_BUILD" != "true" ]]; then
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
  echo "Construyendo y publicando $SERVICE..."
  echo "Directorio: $SERVICE_DIR"

  pushd "$SERVICE_DIR" >/dev/null

  if [[ -x "build-image.bash" ]]; then
    ./build-image.bash
  else
    TAG="${SERVICE_TAGS[$SERVICE]:-latest}"
    if [[ -f "requirements.txt" ]]; then
      echo "Proyecto Python detectado, saltando Maven..."
    else
      ./mvnw clean package -DskipTests
    fi

    docker build -t "stevenrq/$SERVICE:$TAG" .
    docker push "stevenrq/$SERVICE:$TAG"
  fi

  popd >/dev/null
  echo "Build/push finalizado para $SERVICE."
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

if ! docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" config --services | grep -qx "$SERVICE"; then
  echo "Servicio no definido en $COMPOSE_FILE: $SERVICE" >&2
  popd >/dev/null
  exit 1
fi

if [[ "$PULL" == "true" ]]; then
  docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" pull "$SERVICE"
fi

NO_DEPS_ARGS=()
if [[ "$WITH_DEPS" != "true" ]]; then
  NO_DEPS_ARGS=(--no-deps)
fi

docker compose -f "$COMPOSE_FILE" "${ENV_ARGS[@]}" up -d "${NO_DEPS_ARGS[@]}" --force-recreate "$SERVICE"

popd >/dev/null

echo "Contenedor $SERVICE recreado en Compose."
