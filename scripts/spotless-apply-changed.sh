#!/usr/bin/env bash
# Hook de pre-commit: aplica Spotless solo en los módulos Java con archivos staged.
# Si reformatea algo, el commit falla mostrando el diff; haz `git add -u` y reintenta.
# Escape: SKIP=spotless-java git commit
set -euo pipefail

declare -A modules=()
for f in "$@"; do
	[[ "$f" == apps/backend/*/src/* ]] || continue
	svc="${f#apps/backend/}"
	modules["apps/backend/${svc%%/*}"]=1
done

for m in "${!modules[@]}"; do
	echo "spotless:apply -> $m"
	(cd "$m" && ./mvnw -B -q spotless:apply)
done
