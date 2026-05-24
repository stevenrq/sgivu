#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'EOF' >&2
Uso: ./dev-up.sh [--with <servicio>]... [--down|--stop] [-h|--help]

Levanta el stack de infraestructura de desarrollo (Postgres, Redis, Config,
Discovery) usando docker-compose.infra.yml + .env.dev. El microservicio que
estés editando debe correr en el host (IDE o `mvn spring-boot:run`) para
aprovechar Spring DevTools y evitar el rebuild de imagen.

Opciones:
  --with <servicio>   Añade un servicio extra del docker-compose.dev.yml
                      al stack (puede repetirse). Útil para correr en Docker
                      servicios que no estás editando (auth, gateway, etc.).
  --down              Apaga el stack y elimina los contenedores (borra sesiones).
  --stop              Para los contenedores sin eliminarlos (preserva sesiones).
  -h, --help          Muestra esta ayuda.

Ejemplos:
  ./dev-up.sh                                    # solo infra base
  ./dev-up.sh --with sgivu-auth                  # infra + auth
  ./dev-up.sh --with sgivu-auth --with sgivu-gateway
  ./dev-up.sh --down                             # apaga y elimina contenedores
  ./dev-up.sh --stop                             # para sin eliminar (preserva sesiones)

Tras levantar la infra, exporta los siguientes env vars en la terminal donde
corras el servicio en host (los valores reales de DB/Redis vienen de .env.dev):

  export SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
  export EUREKA_URL=http://localhost:8761/eureka
  export DEV_<SERVICIO>_DB_HOST=localhost
  export DEV_REDIS_HOST=localhost

Luego: cd apps/backend/<servicio> && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
EOF
	exit "${1:-1}"
}

EXTRA_SERVICES=()
ACTION="up"

while [[ $# -gt 0 ]]; do
	case "$1" in
	--with)
		[[ -n "${2:-}" ]] || { echo "Error: --with requiere un nombre de servicio" >&2; usage 1; }
		EXTRA_SERVICES+=("$2")
		shift 2
		;;
	--down)
		ACTION="down"
		shift
		;;
	--stop)
		ACTION="stop"
		shift
		;;
	-h | --help)
		usage 0
		;;
	*)
		echo "Argumento no reconocido: $1" >&2
		usage 1
		;;
	esac
done

cd "$(dirname "${BASH_SOURCE[0]}")"

COMPOSE_FILES=(-f docker-compose.infra.yml)
if [[ ${#EXTRA_SERVICES[@]} -gt 0 ]]; then
	COMPOSE_FILES+=(-f docker-compose.dev.yml)
fi

if [[ "$ACTION" == "down" ]]; then
	echo "Apagando stack de desarrollo (elimina contenedores)..."
	docker compose "${COMPOSE_FILES[@]}" --env-file .env.dev down
	exit 0
fi

if [[ "$ACTION" == "stop" ]]; then
	echo "Parando stack de desarrollo (preserva contenedores y sesiones)..."
	docker compose "${COMPOSE_FILES[@]}" --env-file .env.dev stop
	exit 0
fi

BASE_SERVICES=(sgivu-postgres sgivu-redis sgivu-config sgivu-discovery)
ALL_SERVICES=("${BASE_SERVICES[@]}" "${EXTRA_SERVICES[@]}")

echo "Levantando: ${ALL_SERVICES[*]}"
docker compose "${COMPOSE_FILES[@]}" --env-file .env.dev up -d "${ALL_SERVICES[@]}"

echo ""
echo "Stack de desarrollo arriba. Endpoints expuestos en localhost:"
echo "  - Postgres:  localhost:5432"
echo "  - Redis:     localhost:6379"
echo "  - Config:    http://localhost:8888"
echo "  - Discovery: http://localhost:8761"
if [[ ${#EXTRA_SERVICES[@]} -gt 0 ]]; then
	echo "  - Extras:    ${EXTRA_SERVICES[*]}"
fi
echo ""
echo "Para correr un servicio en host (ejemplo sgivu-user):"
echo "  cd apps/backend/sgivu-user"
echo "  SPRING_CONFIG_IMPORT=configserver:http://localhost:8888 \\"
echo "  EUREKA_URL=http://localhost:8761/eureka \\"
echo "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo "Si necesitas reconstruir una imagen, usa: ./rebuild-service.sh --dev <servicio>"
