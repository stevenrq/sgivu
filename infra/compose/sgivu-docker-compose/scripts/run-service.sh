#!/usr/bin/env bash
set -euo pipefail

usage() {
  # usage [exit_code]
  # Muestra la ayuda y sale con el código pasado (por defecto 1).
  local _code="${1:-1}"
  cat <<'EOF' >&2
Uso: $0 [--dev|--prod] SERVICIO [SERVICIO...]
Levanta uno o varios servicios definidos en docker-compose en modo detenido (detached).

Ejemplos:
  $0 --dev sgivu-auth
  $0 sgivu-gateway --prod
  $0 --dev sgivu-auth sgivu-user

Opciones:
  --dev|-d    Usar docker-compose.dev.yml y .env.dev
  --prod|-p   Usar docker-compose.yml y .env (por defecto)
  -h|--help   Mostrar esta ayuda
EOF
  exit "$_code"
}

# `MODE_SPECIFIED` indica si se pasó explícitamente --dev o --prod
MODE=prod
MODE_SPECIFIED=0
REMAIN=()
for arg in "$@"; do
  case "$arg" in
    --dev|-d)
      MODE=dev
      MODE_SPECIFIED=1
      ;;
    --prod|-p)
      MODE=prod
      MODE_SPECIFIED=1
      ;;
    -h|--help)
      usage 0
      ;;
    *)
      REMAIN+=("$arg")
      ;;
  esac
done

# Exigir que el modo sea pasado explícitamente
if [ "${MODE_SPECIFIED}" -eq 0 ]; then
  echo "Error: debe proporcionar la bandera --dev o --prod." >&2
  usage 1
fi

if [ "${#REMAIN[@]}" -eq 0 ]; then
  echo "Error: faltan nombres de SERVICIO." >&2
  usage 1
fi

COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"

if [ "$MODE" = "dev" ]; then
  if [ -f "docker-compose.dev.yml" ]; then
    COMPOSE_FILE="docker-compose.dev.yml"
  else
    echo "Aviso: docker-compose.dev.yml no encontrado, usando docker-compose.yml" >&2
  fi
  if [ -f ".env.dev" ]; then
    ENV_FILE=".env.dev"
  else
    echo "Aviso: .env.dev no encontrado, usando .env" >&2
  fi
fi

echo "Iniciando servicios: ${REMAIN[*]}"
echo "Archivo compose: $COMPOSE_FILE"
echo "Archivo de variables: $ENV_FILE"

# Levantar los servicios solicitados (usa imágenes ya construidas; sin --build)
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d "${REMAIN[@]}"

# Mostrar el estado de los servicios solicitados
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps "${REMAIN[@]}"

echo ""
echo "Si necesitas reconstruir una imagen, usa: ./rebuild-service.sh --dev <servicio>"

exit 0
