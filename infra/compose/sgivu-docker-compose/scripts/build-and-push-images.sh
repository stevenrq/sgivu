#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
STATUS=0

# Orden explícito de build/push
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

# Directorios relativos por servicio
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

# Tag por servicio
declare -A SERVICES_TAGS=(
  [sgivu-auth]="latest"
  [sgivu-client]="latest"
  [sgivu-config]="latest"
  [sgivu-discovery]="latest"
  [sgivu-gateway]="latest"
  [sgivu-user]="latest"
  [sgivu-vehicle]="latest"
  [sgivu-purchase-sale]="latest"
  [sgivu-ml]="latest"
)

for SERVICE in "${SERVICES[@]}"; do
  TAG="${SERVICES_TAGS[$SERVICE]:-latest}"
  REL_DIR="${SERVICE_DIRS[$SERVICE]:-}"
  SERVICE_DIR="$ROOT_DIR/$REL_DIR"

  echo "=============================="
  echo "Construyendo y subiendo $SERVICE con tag $TAG..."
  echo "Directorio: $SERVICE_DIR"

  if [[ -z "$REL_DIR" || ! -d "$SERVICE_DIR" ]]; then
    echo "Carpeta $SERVICE_DIR no existe (skip $SERVICE)"
    STATUS=1
    continue
  fi

  pushd "$SERVICE_DIR" >/dev/null || { STATUS=1; continue; }

  if [[ -x "build-image.sh" ]]; then
    echo "Ejecutando build-image.sh para $SERVICE..."
    if ! ./build-image.sh --push; then
      echo "build-image.sh falló para $SERVICE"
      STATUS=1
      popd >/dev/null
      continue
    fi
    popd >/dev/null
    echo "$SERVICE completado"
    continue
  fi

  if [[ -f "requirements.txt" ]]; then
    echo "Proyecto Python detectado, saltando Maven..."
  else
    echo "Ejecutando Maven..."
    if ! ./mvnw clean package -DskipTests; then
      echo "Maven falló para $SERVICE"
      STATUS=1
      popd >/dev/null
      continue
    fi
  fi

  echo "Construyendo Docker..."
  if ! docker build -t "stevenrq/$SERVICE:$TAG" .; then
    echo "Docker build falló para $SERVICE"
    STATUS=1
    popd >/dev/null
    continue
  fi

  echo "Subiendo Docker..."
  if ! docker push "stevenrq/$SERVICE:$TAG"; then
    echo "Docker push falló para $SERVICE"
    STATUS=1
    popd >/dev/null
    continue
  fi

  popd >/dev/null
  echo "$SERVICE completado"

done

echo "=============================="
if [[ "$STATUS" -eq 0 ]]; then
  echo "Todos los microservicios han sido construidos y subidos."
else
  echo "Finalizó con errores (revisa logs arriba)."
fi

exit "$STATUS"
