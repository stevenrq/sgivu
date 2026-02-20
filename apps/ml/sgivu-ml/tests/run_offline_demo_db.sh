#!/usr/bin/env bash
set -euo pipefail

# Ejecuta la prueba offline usando base de datos (modo principal).
# - ENV_FILE: ruta opcional a archivo .env que se desea cargar.

PYTHON_BIN=${PYTHON_BIN:-python3}
command -v "$PYTHON_BIN" >/dev/null 2>&1 || { echo "No se encontro $PYTHON_BIN en PATH"; exit 1; }

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE=${ENV_FILE:-}
ENV_LOADED=false
PRESET_DATABASE_URL="${DATABASE_URL:-}"
PRESET_DEV_ML_DB_HOST="${DEV_ML_DB_HOST:-}"
PRESET_DEV_ML_DB_PORT="${DEV_ML_DB_PORT:-}"
PRESET_DEV_ML_DB_NAME="${DEV_ML_DB_NAME:-}"
PRESET_DEV_ML_DB_USERNAME="${DEV_ML_DB_USERNAME:-}"
PRESET_DEV_ML_DB_PASSWORD="${DEV_ML_DB_PASSWORD:-}"
IN_DOCKER=false
if [[ -f "/.dockerenv" ]] || grep -qa 'docker' /proc/1/cgroup 2>/dev/null; then
  IN_DOCKER=true
fi
if [[ -n "${ENV_FILE}" ]]; then
  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "ENV_FILE no existe: ${ENV_FILE}" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
  ENV_LOADED=true
elif [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ROOT_DIR}/.env"
  set +a
  ENV_LOADED=true
fi

if [[ "${ENV_LOADED}" == "false" ]]; then
  REPO_ROOT="$(cd "${ROOT_DIR}/../../.." && pwd)"
  COMPOSE_DIR="${REPO_ROOT}/infra/compose/sgivu-docker-compose"
  if [[ -f "${COMPOSE_DIR}/.env.dev" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${COMPOSE_DIR}/.env.dev"
    set +a
  elif [[ -f "${COMPOSE_DIR}/.env" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${COMPOSE_DIR}/.env"
    set +a
  elif [[ -f "${COMPOSE_DIR}/.env.example" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${COMPOSE_DIR}/.env.example"
    set +a
  fi
fi

if [[ -n "${PRESET_DATABASE_URL}" ]]; then
  export DATABASE_URL="${PRESET_DATABASE_URL}"
fi
if [[ -n "${PRESET_DEV_ML_DB_HOST}" ]]; then
  export DEV_ML_DB_HOST="${PRESET_DEV_ML_DB_HOST}"
fi
if [[ -n "${PRESET_DEV_ML_DB_PORT}" ]]; then
  export DEV_ML_DB_PORT="${PRESET_DEV_ML_DB_PORT}"
fi
if [[ -n "${PRESET_DEV_ML_DB_NAME}" ]]; then
  export DEV_ML_DB_NAME="${PRESET_DEV_ML_DB_NAME}"
fi
if [[ -n "${PRESET_DEV_ML_DB_USERNAME}" ]]; then
  export DEV_ML_DB_USERNAME="${PRESET_DEV_ML_DB_USERNAME}"
fi
if [[ -n "${PRESET_DEV_ML_DB_PASSWORD}" ]]; then
  export DEV_ML_DB_PASSWORD="${PRESET_DEV_ML_DB_PASSWORD}"
fi

if [[ -z "${DATABASE_URL:-}" ]]; then
  if [[ -z "${DEV_ML_DB_HOST:-}" || -z "${DEV_ML_DB_NAME:-}" || -z "${DEV_ML_DB_USERNAME:-}" ]]; then
    echo "Faltan variables de base de datos: DATABASE_URL o DEV_ML_DB_*." >&2
    exit 1
  fi
  if [[ "${IN_DOCKER}" == "false" && "${DEV_ML_DB_HOST}" == "sgivu-postgres" ]]; then
    export DEV_ML_DB_HOST="localhost"
    if [[ -z "${DEV_ML_DB_PORT:-}" || "${DEV_ML_DB_PORT}" == "5432" ]]; then
      export DEV_ML_DB_PORT="5433"
    fi
    echo "Usando base de datos en host local: ${DEV_ML_DB_HOST}:${DEV_ML_DB_PORT}" >&2
  fi
  if command -v getent >/dev/null 2>&1; then
    if ! getent hosts "${DEV_ML_DB_HOST}" >/dev/null 2>&1; then
      echo "No se pudo resolver el host ${DEV_ML_DB_HOST}." >&2
      echo "Define DEV_ML_DB_HOST=localhost o usa ENV_FILE con ese valor." >&2
      exit 1
    fi
  fi
fi

export PYTHONPATH="${PYTHONPATH:-$ROOT_DIR}"

"$PYTHON_BIN" "$ROOT_DIR/tests/csv_offline_demo.py" \
  --csv "$ROOT_DIR/tests/data/synthetic_contracts.csv" \
  --horizon 6 \
  --vehicle-type MOTORCYCLE \
  --brand Yamaha \
  --model "MT-03" \
  --line "MT" \
  --plot "$ROOT_DIR/tests/data/forecast.png"
