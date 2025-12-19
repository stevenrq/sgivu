#!/usr/bin/env bash
set -euo pipefail

# Verifica que la versión de Spring Boot documentada en cada README coincida con la del pom.xml
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATUS=0

SERVICES=(
  apps/backend/sgivu-auth
  apps/backend/sgivu-client
  apps/backend/sgivu-config
  apps/backend/sgivu-discovery
  apps/backend/sgivu-gateway
  apps/backend/sgivu-purchase-sale
  apps/backend/sgivu-user
  apps/backend/sgivu-vehicle
)

extract_pom_version() {
  local pom_file="$1"
  perl -0777 -ne 'if(/<parent>\s*<groupId>org\.springframework\.boot<\/groupId>\s*<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>([^<]+)<\/version>/s){print $1}' "$pom_file"
}

extract_readme_version() {
  local readme_file="$1"
  perl -ne 'if(/Spring Boot\s+([0-9]+\.[0-9]+\.[0-9]+)/){print $1; exit}' "$readme_file"
}

for SERVICE in "${SERVICES[@]}"; do
  POM="$ROOT_DIR/$SERVICE/pom.xml"
  README="$ROOT_DIR/$SERVICE/README.md"

  if [[ ! -f "$POM" ]]; then
    echo "SKIP $SERVICE (sin pom.xml)"
    continue
  fi

  if [[ ! -f "$README" ]]; then
    echo "SKIP $SERVICE (sin README.md)"
    continue
  fi

  pom_version="$(extract_pom_version "$POM" || true)"
  readme_version="$(extract_readme_version "$README" || true)"

  if [[ -z "$pom_version" || -z "$readme_version" ]]; then
    echo "WARN $SERVICE (sin version detectable en README/pom)"
    STATUS=1
    continue
  fi

  if [[ "$pom_version" == "$readme_version" ]]; then
    echo "MATCH $SERVICE ($pom_version)"
  else
    echo "MISMATCH $SERVICE (README $readme_version vs pom $pom_version)"
    STATUS=1
  fi

done

exit "$STATUS"
