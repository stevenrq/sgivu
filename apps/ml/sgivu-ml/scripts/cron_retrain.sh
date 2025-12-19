#!/usr/bin/env bash
set -euo pipefail

# Script para ser usado por cron dentro del contenedor sgivu-ml.
# Requiere que la variable TOKEN contenga un JWT valido emitido por sgivu-auth.

API_URL=${API_URL:-"http://localhost:8000/v1/retrain"}
START_DATE=${START_DATE:-}
END_DATE=${END_DATE:-}

body="{"
if [[ -n "${START_DATE}" ]]; then
  body+="\"start_date\":\"${START_DATE}\","
fi
if [[ -n "${END_DATE}" ]]; then
  body+="\"end_date\":\"${END_DATE}\","
fi
body="${body%,}"
body+="}"

curl -sS -X POST "${API_URL}" \
  -H "Authorization: Bearer ${TOKEN:?Debe exportar TOKEN con un JWT valido}" \
  -H "Content-Type: application/json" \
  -d "${body}"
