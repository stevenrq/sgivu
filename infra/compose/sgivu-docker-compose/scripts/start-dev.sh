#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'EOF' >&2
Uso: ./start-dev.sh [--down] [--stop] [--status] [-h|--help]

Levanta el entorno de desarrollo completo:
  1. Infra en Docker  (dev-up.sh)     → Postgres, Redis, Config, Discovery
  2. Apps en host     (host-run.sh)   → auth, client, user, vehicle,
                                         purchase-sale, gateway, ml
                                         (hot-reload via DevTools / uvicorn)

Opciones:
  --down      Apaga toda la infra Docker (dev-up.sh --down).
  --stop      Detiene todos los servicios host (host-run.sh --stop).
  --all-down  Hace --stop y luego --down (apaga todo).
  --status    Muestra el estado de los servicios host.
  -h, --help  Muestra esta ayuda.

Sin opciones levanta todo el stack (infra Docker + apps en host).

Ejemplos:
  ./start-dev.sh              # levanta todo
  ./start-dev.sh --status     # estado de los servicios host
  ./start-dev.sh --stop       # detiene apps host (infra Docker sigue)
  ./start-dev.sh --down       # apaga infra Docker (apps host deben detenerse primero)
  ./start-dev.sh --all-down   # detiene apps host y apaga infra Docker
EOF
	exit "${1:-1}"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

wait_for_http() {
	local url="$1"
	local label="${2:-$1}"
	local timeout_seconds="${3:-90}"
	local elapsed=0

	printf "  Esperando %-28s " "$label..."
	while (( elapsed < timeout_seconds )); do
		if curl -fsS "$url" >/dev/null 2>&1; then
			echo "OK (${elapsed}s)"
			return 0
		fi
		sleep 2
		elapsed=$(( elapsed + 2 ))
	done
	echo "TIMEOUT"
	return 1
}

# ── Parseo de argumentos ────────────────────────────────────────────────────

ACTION="start"

case "${1:-}" in
	--down)    ACTION="down"     ;;
	--stop)    ACTION="stop"     ;;
	--all-down) ACTION="all-down" ;;
	--status)  ACTION="status"   ;;
	-h|--help) usage 0           ;;
	"")        ACTION="start"    ;;
	*)         echo "Opción desconocida: $1" >&2; usage 1 ;;
esac

cd "$SCRIPT_DIR"

# ── Acciones de apagado / estado ───────────────────────────────────────────

case "$ACTION" in
	status)
		./host-run.sh --status
		exit 0
		;;
	stop)
		echo "Deteniendo servicios host..."
		./host-run.sh --stop
		exit 0
		;;
	down)
		echo "Apagando infra Docker..."
		./dev-up.sh --down
		exit 0
		;;
	all-down)
		echo "Deteniendo servicios host..."
		./host-run.sh --stop
		echo "Apagando infra Docker..."
		./dev-up.sh --down
		exit 0
		;;
esac

# ── Arranque ────────────────────────────────────────────────────────────────

echo "╔══════════════════════════════════════════╗"
echo "║       SGIVU — Entorno de desarrollo      ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# 1. Infra Docker
echo "▶ Paso 1/3 — Infra Docker (Postgres · Redis · Config · Discovery)"
./dev-up.sh
echo ""

# 2. Esperar que Config y Discovery estén listos antes de lanzar las apps
echo "▶ Paso 2/3 — Esperando servicios de infra..."
wait_for_http "http://localhost:8888/actuator/health" "Config Server (:8888)"  90 || {
	echo "  ERROR: sgivu-config no arrancó. Revisa: docker compose logs sgivu-config" >&2
	exit 1
}
wait_for_http "http://localhost:8761/actuator/health" "Discovery (:8761)"      90 || {
	echo "  ERROR: sgivu-discovery no arrancó. Revisa: docker compose logs sgivu-discovery" >&2
	exit 1
}
echo ""

# 3. Apps en host (en orden: auth → resource servers → gateway → ml)
echo "▶ Paso 3/3 — Lanzando apps en host..."
./host-run.sh --all
echo ""

# ── Resumen ─────────────────────────────────────────────────────────────────

echo "══════════════════════════════════════════════"
echo "  Stack completo arriba"
echo "══════════════════════════════════════════════"
echo "  Infra Docker:"
echo "    Postgres   → localhost:5432"
echo "    Redis      → localhost:6379"
echo "    Config     → http://localhost:8888"
echo "    Discovery  → http://localhost:8761"
echo ""
echo "  Apps en host (hot-reload activo):"
echo "    Auth       → http://localhost:9000"
echo "    Client     → http://localhost:8082"
echo "    User       → http://localhost:8081"
echo "    Vehicle    → http://localhost:8083"
echo "    PurchSale  → http://localhost:8084"
echo "    Gateway    → http://localhost:8080"
echo "    ML         → http://localhost:8000"
echo ""
echo "  Comandos útiles:"
echo "    ./start-dev.sh --status           # estado de los servicios host"
echo "    ./host-run.sh --logs <servicio>   # tail -f del log"
echo "    ./start-dev.sh --stop             # detiene apps host"
echo "    ./start-dev.sh --all-down         # detiene todo"
