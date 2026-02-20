#!/usr/bin/env bash
set -euo pipefail

usage() {
	echo "Usage: $0 --dev|--prod" >&2
	exit 1
}

case "${1:-}" in
	--dev|-d)
		echo "Starting dev stack..."
		docker compose -f docker-compose.dev.yml --env-file .env.dev up -d --build
		;;
	--prod|-p)
		echo "Starting prod stack..."
		docker compose up -d --build
		;;
	*)
		usage
		;;
esac