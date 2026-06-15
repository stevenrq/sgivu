#!/usr/bin/env bash
# ----------------------------------------------------------------
# dbs_backup.sh - Copia de seguridad automatica
# PostgreSQL (sgivu-postgres), Redis (sgivu-redis)
# ----------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
  cat <<EOF
Uso: $0 --dev|--prod [--backup-root DIR] [--retain DAYS]

Opciones:
  --dev            Usar .env.dev
  --prod           Usar .env
  --backup-root    Sobrescribir directorio de backups
  --retain DAYS    Eliminar archivos con más de DAYS días (por defecto 7)
  -h, --help       Muestra esta ayuda
EOF
  exit 1
}

ENV_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dev) ENV_FILE=".env.dev"; shift ;;
    --prod) ENV_FILE=".env"; shift ;;
    --backup-root) BACKUP_ROOT="$2"; shift 2 ;;
    --retain|--retention) RETENTION_DAYS="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Opción desconocida: $1"; usage ;;
  esac
done

if [[ -z "${ENV_FILE}" ]]; then
  echo "Error: debes indicar --dev o --prod"
  usage
fi

ENV_PATH="$COMPOSE_DIR/$ENV_FILE"
if [[ ! -f "$ENV_PATH" ]]; then
  echo "Error: archivo de entorno no encontrado: $ENV_PATH"
  exit 2
fi

# Cargar variables de .env seleccionado (export)
set -o allexport
# shellcheck disable=SC1090
source "$ENV_PATH"
set +o allexport

# -- Configuración -------------------------------------------
BACKUP_ROOT="${BACKUP_ROOT:-$HOME/Backups/sgivu-dbs-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"          # Días que se conservan los backups
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$BACKUP_ROOT/backup.log"

# Credenciales (ajusta o carga desde .env)
POSTGRES_USER="${POSTGRES_USER}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD}"
REDIS_PASSWORD="${REDIS_PASSWORD}"

# Bases de datos de PostgreSQL a respaldar
PG_DATABASES=(
  sgivu_auth_db
  sgivu_user_db
  sgivu_client_db
  sgivu_vehicle_db
  sgivu_purchase_sale_db
  sgivu_ml_db
)

# -- Funciones auxiliares ------------------------------------
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"; }

check_container() {
  if ! docker inspect --format='{{.State.Running}}' "$1" 2>/dev/null | grep -q true; then
    log "Contenedor '$1' no está corriendo. Saltando..."
    return 1
  fi
  return 0
}

# -- Preparar directorios ------------------------------------
mkdir -p "$BACKUP_ROOT"/{postgres,redis}

log "==================================================="
log "Iniciando backup SGIVU - $TIMESTAMP"
log "==================================================="

# -- 1. PostgreSQL (sgivu-postgres) ---------------------------
if check_container sgivu-postgres; then
  PG_DIR="$BACKUP_ROOT/postgres"

  for db in "${PG_DATABASES[@]}"; do
    PG_FILE="$PG_DIR/${db}_$TIMESTAMP.sql.gz"
    log "PostgreSQL: respaldando $db..."

    docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" sgivu-postgres \
      pg_dump -U "$POSTGRES_USER" -d "$db" \
      --format=plain --no-owner --no-privileges | gzip > "$PG_FILE"

    log "PostgreSQL: $(du -h "$PG_FILE" | cut -f1) → $PG_FILE"
  done
fi

# -- 2. Redis (sgivu-redis) -----------------------------------
if check_container sgivu-redis; then
  REDIS_DIR="$BACKUP_ROOT/redis"
  REDIS_FILE="$REDIS_DIR/dump_$TIMESTAMP.rdb"

  log "Redis: disparando BGSAVE..."
  docker exec sgivu-redis \
    redis-cli -a "$REDIS_PASSWORD" --no-auth-warning BGSAVE

  # Esperar a que BGSAVE termine (máximo 60 segundos)
  for _ in $(seq 1 60); do
    BG_STATUS=$(docker exec sgivu-redis \
      redis-cli -a "$REDIS_PASSWORD" --no-auth-warning INFO persistence \
      | grep rdb_bgsave_in_progress | tr -d '[:space:]')
    if [[ "$BG_STATUS" == "rdb_bgsave_in_progress:0" ]]; then
      break
    fi
    sleep 1
  done

  docker cp sgivu-redis:/data/dump.rdb "$REDIS_FILE"
  gzip "$REDIS_FILE"

  log "Redis: $(du -h "${REDIS_FILE}.gz" | cut -f1) → ${REDIS_FILE}.gz"
fi

# -- 3. Limpieza de backups antiguos --------------------------
log "Eliminando backups con más de $RETENTION_DAYS días..."
find "$BACKUP_ROOT" -type f \( -name "*.gz" -o -name "*.rdb" \) \
  -mtime +"$RETENTION_DAYS" -delete -print | while read -r f; do
  log "   Eliminado: $f"
done

log "==================================================="
log "Backup finalizado."
log "==================================================="
