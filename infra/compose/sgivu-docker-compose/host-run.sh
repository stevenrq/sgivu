#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'EOF' >&2
Uso: ./host-run.sh <servicio> [<servicio>...] [--stop|--status|--logs <svc>]

Ejecuta uno o varios microservicios SGIVU en la máquina host (fuera de
Docker), apuntando a la infra dockerizada en localhost. Cada servicio se
lanza en background y sus logs van a .host-run/<servicio>.log, su PID a
.host-run/<servicio>.pid.

El comando se elige según el tipo de proyecto:
  - Java (mvnw)         -> ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
                            (hot-reload con Spring DevTools)
  - Python (requirements.txt) -> .venv/bin/uvicorn app.main:app --reload
                            (hot-reload nativo de uvicorn; venv autocreado)

Servicios soportados:
  Java (Spring Boot, hot-reload via DevTools):
    sgivu-auth, sgivu-client, sgivu-gateway, sgivu-user,
    sgivu-vehicle, sgivu-purchase-sale, sgivu-config, sgivu-discovery
  Python (FastAPI, hot-reload via uvicorn --reload):
    sgivu-ml

Opciones:
  --all               Lanza el stack completo de apps en host: auth, client,
                      user, vehicle, purchase-sale, gateway, ml. (config y
                      discovery se asumen en Docker vía dev-up.sh).
  --stop [<svc>...]   Detiene los servicios indicados (o todos si no se pasan).
  --status            Muestra qué servicios están vivos y sus PIDs.
  --logs <svc>        Hace tail -f del log del servicio indicado.
  -h, --help          Muestra esta ayuda.

Ejemplos:
  ./host-run.sh sgivu-user                       # solo user en host
  ./host-run.sh sgivu-user sgivu-auth            # user y auth en host
  ./host-run.sh --all                            # todas las apps en host
  ./host-run.sh --status
  ./host-run.sh --logs sgivu-user
  ./host-run.sh --stop                           # detiene todos
  ./host-run.sh --stop sgivu-user                # detiene solo user

Pre-requisito: la infra debe estar arriba (`./dev-up.sh`).
EOF
	exit "${1:-1}"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
RUN_DIR="$SCRIPT_DIR/.host-run"
ENV_FILE="$SCRIPT_DIR/.env.dev"

# uv instala en ~/.local/bin, que no siempre está en PATH en shells no-interactivas
export PATH="$HOME/.local/bin:$PATH"

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

mkdir -p "$RUN_DIR"

is_alive() {
	local pid_file="$RUN_DIR/$1.pid"
	[[ -f "$pid_file" ]] || return 1
	local pid
	pid="$(cat "$pid_file")"
	[[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

cmd_status() {
	local any=0
	for svc in "${!SERVICE_DIRS[@]}"; do
		if is_alive "$svc"; then
			printf "  %-22s UP    PID %s    log: %s\n" \
				"$svc" "$(cat "$RUN_DIR/$svc.pid")" "$RUN_DIR/$svc.log"
			any=1
		fi
	done
	[[ "$any" -eq 0 ]] && echo "  (ningún servicio host activo)"
	return 0
}

cmd_stop() {
	local targets=("$@")
	if [[ ${#targets[@]} -eq 0 ]]; then
		targets=("${!SERVICE_DIRS[@]}")
	fi
	for svc in "${targets[@]}"; do
		local pid_file="$RUN_DIR/$svc.pid"
		if is_alive "$svc"; then
			local pid
			pid="$(cat "$pid_file")"
			echo "Deteniendo $svc (PID $pid)..."
			kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
			# Esperar hasta 10s a que muera
			for _ in 1 2 3 4 5 6 7 8 9 10; do
				kill -0 "$pid" 2>/dev/null || break
				sleep 1
			done
			kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
			rm -f "$pid_file"
		else
			rm -f "$pid_file" 2>/dev/null || true
		fi
	done
}

cmd_logs() {
	local svc="${1:-}"
	[[ -n "$svc" ]] || { echo "Error: --logs requiere un nombre de servicio" >&2; usage 1; }
	local log_file="$RUN_DIR/$svc.log"
	[[ -f "$log_file" ]] || { echo "No hay log para $svc en $log_file" >&2; exit 1; }
	exec tail -f "$log_file"
}

wait_for_http() {
	local url="$1"
	local timeout_seconds="${2:-60}"
	local elapsed=0

	while (( elapsed < timeout_seconds )); do
		if curl -fsS "$url" >/dev/null 2>&1; then
			return 0
		fi
		sleep 1
		elapsed=$((elapsed + 1))
	done

	return 1
}

check_uv() {
	command -v uv >/dev/null 2>&1 && return 0
	echo "ERROR: 'uv' no encontrado. Instálalo con:" >&2
	echo "  curl -LsSf https://astral.sh/uv/install.sh | sh" >&2
	exit 1
}

# Argument parsing
ACTION="run"
TARGETS=()

# Stack completo de aplicaciones (excluye config y discovery: esos van en
# Docker vía dev-up.sh). Orden importa: auth primero (OAuth2 issuer), luego
# resource servers, luego gateway y ml.
ALL_APP_SERVICES=(
	sgivu-auth
	sgivu-client
	sgivu-user
	sgivu-vehicle
	sgivu-purchase-sale
	sgivu-gateway
	sgivu-ml
)

while [[ $# -gt 0 ]]; do
	case "$1" in
		-h | --help) usage 0 ;;
		--status) ACTION="status"; shift ;;
		--stop) ACTION="stop"; shift; while [[ $# -gt 0 ]]; do TARGETS+=("$1"); shift; done ;;
		--logs) ACTION="logs"; shift; TARGETS+=("${1:-}"); shift || true ;;
		--all) TARGETS+=("${ALL_APP_SERVICES[@]}"); shift ;;
		--*) echo "Opción desconocida: $1" >&2; usage 1 ;;
		*) TARGETS+=("$1"); shift ;;
	esac
done

case "$ACTION" in
	status) cmd_status; exit 0 ;;
	stop)   cmd_stop "${TARGETS[@]}"; exit 0 ;;
	logs)   cmd_logs "${TARGETS[@]:-}" ;;
esac

# ACTION=run
[[ ${#TARGETS[@]} -gt 0 ]] || TARGETS+=("${ALL_APP_SERVICES[@]}")

# Cargar .env.dev y sobreescribir hosts a localhost
[[ -f "$ENV_FILE" ]] || { echo "No existe $ENV_FILE" >&2; exit 1; }
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Overrides incondicionales: .env.dev usa hostnames Docker (sgivu-postgres,
# sgivu-auth, etc.) que no resuelven desde host. Forzamos localhost para que
# las apps en host hablen contra los puertos publicados por dev-up.sh.
export DEV_REDIS_HOST=localhost
export DEV_AUTH_DB_HOST=localhost
export DEV_USER_DB_HOST=localhost
export DEV_CLIENT_DB_HOST=localhost
export DEV_VEHICLE_DB_HOST=localhost
export DEV_PURCHASE_SALE_DB_HOST=localhost
export DEV_ML_DB_HOST=localhost
export SPRING_CONFIG_IMPORT="configserver:http://localhost:8888"
export EUREKA_URL="http://localhost:8761/eureka"
export SGIVU_AUTH_URL="http://localhost:9000"
# En auth se usa RestClient @LoadBalanced para sgivu-user; debe ser service-id,
# no localhost, para que Spring Cloud LoadBalancer/Eureka resuelvan instancias.
export SGIVU_USER_URL="http://sgivu-user:8081"
export SGIVU_ML_URL="http://localhost:8000"
# SGIVU_PURCHASE_SALE_URL y SGIVU_VEHICLE_URL NO se sobreescriben aquí: los servicios
# Spring Boot usan @LoadBalanced RestClient y necesitan el service-id (sgivu-vehicle,
# sgivu-purchase-sale) para que Eureka los resuelva. Solo sgivu-ml (httpx sin LoadBalancer)
# necesita localhost; esos overrides se inyectan en el subshell Python más abajo.
export SGIVU_AUTH_DISCOVERY_URL="http://localhost:9000/.well-known/openid-configuration"
export SGIVU_GATEWAY_URL="http://localhost:8080"
export ISSUER_URL="http://localhost:9000"

for svc in "${TARGETS[@]}"; do
	rel_dir="${SERVICE_DIRS[$svc]:-}"
	if [[ -z "$rel_dir" ]]; then
		echo "Servicio no soportado: $svc" >&2
		echo "Soportados: ${!SERVICE_DIRS[*]}" >&2
		exit 1
	fi
	if is_alive "$svc"; then
		echo "$svc ya está corriendo (PID $(cat "$RUN_DIR/$svc.pid")). Salto."
		continue
	fi

	svc_dir="$ROOT_DIR/$rel_dir"
	[[ -d "$svc_dir" ]] || { echo "No existe $svc_dir" >&2; exit 1; }

	log_file="$RUN_DIR/$svc.log"
	pid_file="$RUN_DIR/$svc.pid"

	echo "Lanzando $svc en host..."
	echo "  dir:  $svc_dir"
	echo "  log:  $log_file"

	if [[ "$svc" == "sgivu-gateway" ]]; then
		echo "  esperando issuer OIDC de sgivu-auth en ${SGIVU_AUTH_URL}..."
		if ! wait_for_http "${SGIVU_AUTH_URL}/.well-known/openid-configuration" 60; then
			echo "  ERROR: sgivu-auth no está listo en ${SGIVU_AUTH_URL}. No se inicia sgivu-gateway." >&2
			echo "  Revisa: $RUN_DIR/sgivu-auth.log" >&2
			continue
		fi
	fi

	if [[ -f "$svc_dir/requirements.txt" ]]; then
		# Servicio Python (FastAPI) — hot-reload via uvicorn --reload
		check_uv
		if [[ ! -d "$svc_dir/.venv" || ! -x "$svc_dir/.venv/bin/python" ]]; then
			echo "  venv: creando con uv (Python 3.12)..."
			uv venv --python 3.12 "$svc_dir/.venv" >>"$log_file" 2>&1
		fi
		echo "  deps: uv pip install -r requirements.txt (idempotente)..."
		uv pip install -q --python "$svc_dir/.venv/bin/python" \
			-r "$svc_dir/requirements.txt" >>"$log_file" 2>&1
		(
			cd "$svc_dir"
			# Servicios Python usan httpx (sin @LoadBalanced): necesitan localhost,
			# no service-ids de Eureka como los RestClient de Spring.
			export SGIVU_PURCHASE_SALE_URL=http://localhost:8084
			export SGIVU_VEHICLE_URL=http://localhost:8083
			# Fuerza salida sin buffer para que los logs de uvicorn lleguen al
			# archivo de log sin esperar a que el buffer se llene.
			export PYTHONUNBUFFERED=1
			setsid .venv/bin/python -m uvicorn app.main:app --reload \
				--host 0.0.0.0 --port "${PORT:-8000}" \
				>>"$log_file" 2>&1 &
			echo $! >"$pid_file"
		)
	elif [[ -x "$svc_dir/mvnw" ]]; then
		# Servicio Java (Spring Boot) — hot-reload via DevTools.
		# Los hosts (config server, eureka, db, redis) vienen de las env vars
		# exportadas arriba (los application.yml y *-dev.yml usan ${VAR:default}).
		(
			cd "$svc_dir"
			# setsid para que tenga su propio process group (facilita kill -- -PID)
			setsid ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
				>"$log_file" 2>&1 &
			echo $! >"$pid_file"
		)
	else
		echo "  ERROR: $svc_dir no tiene mvnw ni requirements.txt; tipo de proyecto desconocido" >&2
		exit 1
	fi

	sleep 1
	if is_alive "$svc"; then
		echo "  PID:  $(cat "$pid_file")"
	else
		echo "  ERROR: $svc no arrancó. Revisa $log_file" >&2
		rm -f "$pid_file"
	fi
done

echo ""
echo "Comandos útiles:"
echo "  ./host-run.sh --status              # ver servicios activos"
echo "  ./host-run.sh --logs <servicio>     # tail -f del log"
echo "  ./host-run.sh --stop                # detener todos"
echo "  ./host-run.sh --stop <servicio>     # detener uno"
