#!/usr/bin/env bash
# Lanza sgivu-ml (FastAPI) en host con uvicorn --reload. Wrapper de host-run.sh.
# Pre-requisito: la infra debe estar arriba (./dev-up.sh).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
exec "$ROOT_DIR/infra/compose/sgivu-docker-compose/scripts/host-run.sh" sgivu-ml "$@"
