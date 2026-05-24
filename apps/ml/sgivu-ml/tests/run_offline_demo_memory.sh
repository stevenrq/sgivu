#!/usr/bin/env bash
set -euo pipefail

# Script para ejecutar una demo offline del modelo de demanda usando un CSV local.

# PYTHON_BIN: intérprete a usar (si no se define, usa .venv/bin/python si existe).
# --csv: ruta al archivo de contratos CSV.
# --horizon: meses a pronosticar.
# --vehicle-type: tipo de vehículo a filtrar (CAR, MOTORCYCLE).
# --brand: marca del vehículo a filtrar.
# --model: modelo del vehículo a filtrar.
# --line: línea del vehículo a filtrar.
# --plot: ruta para guardar la gráfica del pronóstico.
# --help: muestra esta ayuda.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -n "${PYTHON_BIN:-}" ]]; then
	PYTHON_BIN="$PYTHON_BIN"
elif [[ -x "$ROOT_DIR/.venv/bin/python" ]]; then
	PYTHON_BIN="$ROOT_DIR/.venv/bin/python"
else
	PYTHON_BIN="python3"
fi

if [[ "$PYTHON_BIN" == */* ]]; then
	[[ -x "$PYTHON_BIN" ]] || {
		echo "No se encontro ejecutable en $PYTHON_BIN"
		exit 1
	}
else
	command -v "$PYTHON_BIN" >/dev/null 2>&1 || {
		echo "No se encontro $PYTHON_BIN en PATH"
		exit 1
	}
fi

export PYTHONPATH="${PYTHONPATH:-$ROOT_DIR}"

DEFAULT_GENERATED_CSV="$ROOT_DIR/tests/data/synthetic_contracts.csv"
CSV_PATH="$DEFAULT_GENERATED_CSV"
HORIZON=12
VEHICLE_TYPE="MOTORCYCLE"
BRAND="Yamaha"
MODEL="MT-03"
LINE="MT"
PLOT_PATH="$ROOT_DIR/tests/data/forecast.png"

print_help() {
	cat <<EOF
Uso: $(basename "$0") [opciones]

Opciones:
  --csv <ruta>           Ruta al archivo de contratos CSV.
  --horizon <meses>      Meses a pronosticar.
  --vehicle-type <tipo>  Tipo de vehículo (CAR o MOTORCYCLE).
  --brand <marca>        Marca del vehículo.
  --model <modelo>       Modelo del vehículo.
  --line <linea>         Línea del vehículo.
  --plot <ruta>          Ruta para guardar la gráfica.
  --help                 Muestra esta ayuda.
EOF
}

while [[ $# -gt 0 ]]; do
	case "$1" in
		--csv)
			CSV_PATH="$2"
			shift 2
			;;
		--horizon)
			HORIZON="$2"
			shift 2
			;;
		--vehicle-type)
			VEHICLE_TYPE="$2"
			shift 2
			;;
		--brand)
			BRAND="$2"
			shift 2
			;;
		--model)
			MODEL="$2"
			shift 2
			;;
		--line)
			LINE="$2"
			shift 2
			;;
		--plot)
			PLOT_PATH="$2"
			shift 2
			;;
		-h|--help)
			print_help
			exit 0
			;;
		*)
			echo "Argumento no reconocido: $1"
			print_help
			exit 1
			;;
	esac
done

if [[ ! -f "$CSV_PATH" ]]; then
	echo "CSV no encontrado en $CSV_PATH. Generando dataset sintético..."
	"$PYTHON_BIN" "$ROOT_DIR/tests/generate_contracts.py"

	if [[ "$CSV_PATH" != "$DEFAULT_GENERATED_CSV" && -f "$DEFAULT_GENERATED_CSV" ]]; then
		mkdir -p "$(dirname "$CSV_PATH")"
		cp "$DEFAULT_GENERATED_CSV" "$CSV_PATH"
	fi
fi

if [[ ! -f "$CSV_PATH" ]]; then
	echo "No fue posible generar el CSV en $CSV_PATH"
	exit 1
fi

"$PYTHON_BIN" "$ROOT_DIR/tests/csv_offline_demo.py" \
	--csv "$CSV_PATH" \
	--horizon "$HORIZON" \
	--vehicle-type "$VEHICLE_TYPE" \
	--brand "$BRAND" \
	--model "$MODEL" \
	--line "$LINE" \
	--plot "$PLOT_PATH"
