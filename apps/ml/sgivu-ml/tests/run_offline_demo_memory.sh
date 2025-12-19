#!/usr/bin/env bash
set -euo pipefail

# Ejecuta la prueba offline del modelo con el CSV de ejemplo.
# - PYTHON_BIN: intérprete a usar (por defecto python3).
# - --csv: ruta al archivo de contratos CSV.
# - --horizon: meses a pronosticar.
# - --vehicle-type/--brand/--model/--line: segmento que se desea predecir (la línea es obligatoria).
# Ajusta los argumentos si deseas probar otro segmento o archivo. Mantén --line con un valor no vacío.

PYTHON_BIN=${PYTHON_BIN:-python3}
command -v "$PYTHON_BIN" >/dev/null 2>&1 || { echo "No se encontro $PYTHON_BIN en PATH"; exit 1; }

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export PYTHONPATH="${PYTHONPATH:-$ROOT_DIR}"

"$PYTHON_BIN" "$ROOT_DIR/tests/csv_offline_demo.py" \
  --csv "$ROOT_DIR/tests/data/synthetic_contracts.csv" \
  --horizon 6 \
  --vehicle-type MOTORCYCLE \
  --brand Yamaha \
  --model "MT-03" \
  --line "MT" \
  --plot "$ROOT_DIR/tests/data/forecast.png"
