---
name: deploy
description: Despliega a producción (EC2) vía GitHub Actions con make deploy - soporta selección de servicios y rollback por tag, y sigue el run en vivo con make deploy-watch.
disable-model-invocation: true
---

Despliega SGIVU a producción usando el workflow `deploy.yml` de GitHub Actions. Argumentos opcionales (servicios y/o tag): $ARGUMENTS

## Contexto del pipeline

- `make deploy` dispara `deploy.yml`: construye las 9 imágenes, las publica en Docker Hub (`stevenrq/*`) etiquetadas con el SHA corto, y por SSH recrea los contenedores en EC2 con smoke check de 90 s sobre `/actuator/health` del gateway.
- `SERVICES=a,b` limita qué contenedores se recrean (default: `all`).
- `TAG=<sha>` salta el build y despliega imágenes ya publicadas — **modo rollback**.
- El workflow corre sobre el último commit de `main` en GitHub, no sobre el working tree local.

## Pasos

1. **Pre-checks**:
   - `git status` — si hay cambios sin commitear, advierte que NO se incluirán en el deploy.
   - `git fetch origin && git log origin/main -1 --oneline` — confirma qué commit se va a desplegar. Si `main` local está adelante de `origin/main`, advierte que falta hacer push.
   - `gh auth status` — verifica autenticación.

2. **Confirmación**: muestra al usuario exactamente qué se desplegará (commit, servicios, tag si es rollback) y **espera su confirmación explícita antes de disparar**. Es producción.

3. **Disparo**:
   - Deploy normal: `make deploy` o `make deploy SERVICES=sgivu-user,sgivu-gateway`
   - Rollback: `make deploy TAG=<sha-corto> [SERVICES=...]` (sin build; usa imágenes existentes)

4. **Seguimiento**: `make deploy-watch` para seguir el run en vivo. Reporta el resultado de cada job (resolve → build-push → deploy + smoke check).

5. **Si falla**: usa `make ci-status` y `gh run view <run-id> --log-failed` para diagnosticar. Si el deploy dejó producción degradada, ofrece rollback inmediato con el tag del último deploy exitoso (los tags son SHAs cortos de commits anteriores de `main`).
