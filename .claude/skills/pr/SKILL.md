---
name: pr
description: Publica los cambios actuales como pull request - crea rama Gitflow, valida con lint y tests de los servicios afectados, commitea en inglés (Conventional Commits), hace push y abre el PR con gh.
disable-model-invocation: true
---

Publica el trabajo actual como pull request siguiendo las convenciones del repo. Argumentos opcionales del usuario (descripción del cambio, nombre de rama sugerido): $ARGUMENTS

## Pasos

1. **Contexto**: revisa `git status` y `git diff` para entender qué cambió. Si no hay cambios ni commits por publicar, dilo y detente.

2. **Rama**: si estás en `main`, crea una rama nueva según el tipo de cambio: `feature/`, `fix/`, `refactor/`, `chore/`, `docs/` + slug corto en inglés kebab-case (ej: `feature/vehicle-image-upload`). Si ya estás en una rama distinta de `main`, úsala.

3. **Validación por servicios afectados**: identifica los servicios tocados a partir de las rutas de los cambios:
   - `apps/backend/<servicio>/**` → `make lint SERVICE=<servicio>` y `make test SERVICE=<servicio>`
   - `apps/ml/sgivu-ml/**` → `make lint SERVICE=sgivu-ml` y `make test SERVICE=sgivu-ml`
   - Solo `docs/`, `infra/` o archivos raíz → omite los tests; valida compose con `docker compose config -q` si tocaste archivos de compose.

   Si lint falla por formato, corre `make format SERVICE=<servicio>` y continúa. Si los tests fallan, repórtalo y pregunta antes de seguir.

4. **Commit**: commits pequeños y atómicos, en **inglés**, formato Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, ...). Los hooks de pre-commit (Spotless/black/pylint) pueden reformatear archivos: si el commit falla por reformateo, haz `git add -u` y reintenta.

5. **Docs**: si el cambio tiene superficie pública (endpoints, permisos, env vars, esquemas, rutas), aplica el criterio de sincronización de CLAUDE.md — invoca el skill `docs-sync` antes del push si hay páginas afectadas sin actualizar.

6. **Push y PR**: `git push -u origin <rama>` y luego `gh pr create` con base `main`. Título en inglés estilo Conventional Commits; cuerpo con resumen de cambios y cómo se validaron (qué lint/tests corrieron).

7. **Resultado**: reporta la URL del PR y el estado de los checks de CI (`gh pr checks`).
