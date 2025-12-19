#!/usr/bin/env bash
set -euo pipefail

UVICORN_CMD=${UVICORN_CMD:-"uvicorn app.main:app --host 0.0.0.0 --port 8000"}

echo "Starting sgivu-ml with: ${UVICORN_CMD}"
exec ${UVICORN_CMD}
