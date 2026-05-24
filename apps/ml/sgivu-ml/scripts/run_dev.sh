#!/usr/bin/env bash
set -euo pipefail

# Script de conveniencia para ejecutar los scripts de verificación del modelo
# de demanda contra el stack de desarrollo.
#
# Uso:
#   ./scripts/run_dev.sh train [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD]
#   ./scripts/run_dev.sh predict --vehicle-type MOTORCYCLE --brand Yamaha \
#                                --model MT-03 --line MT [--horizon 6] \
#                                [--confidence 0.95] [--plot /tmp/forecast.png]
#
# Requisitos:
#   - Stack dev corriendo: cd infra/compose/sgivu-docker-compose && ./run.sh --dev
#   - El .env.dev de infra/compose/sgivu-docker-compose/ se carga automáticamente.
#     Las URLs Docker internas (sgivu-*) se sustituyen por localhost para acceso
#     desde el host.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# --- Selección del intérprete Python ---
if [[ -n "${PYTHON_BIN:-}" ]]; then
    PYTHON="$PYTHON_BIN"
elif [[ -x "$ROOT_DIR/.venv/bin/python" ]]; then
    PYTHON="$ROOT_DIR/.venv/bin/python"
else
    PYTHON="python3"
fi

if [[ "$PYTHON" == */* ]]; then
    [[ -x "$PYTHON" ]] || {
        echo "No se encontró ejecutable en $PYTHON" >&2
        exit 1
    }
else
    command -v "$PYTHON" >/dev/null 2>&1 || {
        echo "No se encontró $PYTHON en PATH" >&2
        exit 1
    }
fi

export PYTHONPATH="${PYTHONPATH:-$ROOT_DIR}"

# Carga EXCLUSIVAMENTE el .env.dev del stack (infra/compose/sgivu-docker-compose/).
# Nunca se carga .env (producción). Si .env.dev no existe, el script aborta.
COMPOSE_DIR="$(cd "$ROOT_DIR/../../.." && pwd)/infra/compose/sgivu-docker-compose"
ENV_DEV_FILE="$COMPOSE_DIR/.env.dev"

if [[ ! -f "$ENV_DEV_FILE" ]]; then
    echo "ERROR: No se encontró el archivo de desarrollo: $ENV_DEV_FILE" >&2
    echo "Los scripts de verificación solo funcionan con el entorno de desarrollo." >&2
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_DEV_FILE"
set +a

# Garantías de entorno de desarrollo: se sobreescriben siempre, independientemente
# de lo que contenga .env.dev, para evitar cualquier conexión al entorno productivo.
export ENVIRONMENT="dev"
export DATABASE_ENV="dev"
# Los hostnames del .env.dev apuntan a la red interna de Docker (sgivu-*).
# Desde el host, los servicios se exponen en localhost con los mismos puertos.
export SGIVU_PURCHASE_SALE_URL="http://localhost:8084"
export SGIVU_VEHICLE_URL="http://localhost:8083"
export DEV_ML_DB_HOST="localhost"

print_help() {
    cat <<EOF
Uso: $(basename "$0") <subcomando> [opciones]

Subcomandos:
  train    Entrena el modelo cargando contratos desde los microservicios.
  predict  Predice la demanda para un segmento usando el último modelo entrenado.

Opciones para 'train':
  --start-date YYYY-MM-DD   Fecha de inicio para filtrar contratos (opcional).
  --end-date   YYYY-MM-DD   Fecha de fin para filtrar contratos (opcional).

Opciones para 'predict':
  --vehicle-type TIPO        CAR o MOTORCYCLE (obligatorio).
  --brand        MARCA       Marca del vehículo, ej: Yamaha (obligatorio).
  --model        MODELO      Modelo del vehículo, ej: MT-03 (obligatorio).
  --line         LINEA       Línea o submodelo, ej: MT (obligatorio).
  --horizon      MESES       Meses a pronosticar (default: 6).
  --confidence   NIVEL       Nivel de confianza para IC (default: 0.95).
  --plot         RUTA        Ruta para guardar el gráfico PNG (opcional).

Ejemplos:
  ./scripts/run_dev.sh train
  ./scripts/run_dev.sh train --start-date 2024-01-01 --end-date 2025-01-01
  ./scripts/run_dev.sh predict --vehicle-type MOTORCYCLE --brand Yamaha \\
      --model MT-03 --line MT --horizon 6 --plot /tmp/forecast.png
EOF
}

if [[ $# -eq 0 ]]; then
    print_help
    exit 1
fi

SUBCOMMAND="$1"
shift

case "$SUBCOMMAND" in
    train)
        exec "$PYTHON" -m scripts.train_from_services "$@"
        ;;
    predict)
        exec "$PYTHON" -m scripts.predict_from_services "$@"
        ;;
    -h|--help)
        print_help
        exit 0
        ;;
    *)
        echo "Subcomando desconocido: '$SUBCOMMAND'" >&2
        echo "Usa '$(basename "$0") --help' para ver las opciones disponibles." >&2
        exit 1
        ;;
esac
