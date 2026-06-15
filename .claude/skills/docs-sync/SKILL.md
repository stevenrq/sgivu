---
name: docs-sync
description: Revisa el diff actual e identifica y actualiza las páginas Mintlify (docs/**/*.mdx) afectadas por el cambio, según los criterios de sincronización de CLAUDE.md. Usar tras cambios de código con superficie pública - endpoints, permisos, variables de entorno, esquemas de BD, rutas del gateway o flujos ML.
---

Sincroniza la documentación Mintlify de `docs/` con los cambios de código actuales.

## Pasos

1. **Obtén el diff completo**: cambios sin commitear (`git diff` + `git diff --staged`) y, si estás en una rama, lo ya commiteado respecto a main (`git diff main...HEAD`). Lista los archivos tocados.

2. **Clasifica cada cambio** según la tabla de CLAUDE.md:

   | Tipo de cambio | Docs afectados |
   | --- | --- |
   | Endpoint añadido/renombrado/eliminado | `docs/api/**` o `docs/services/**` |
   | Permiso (nombre o semántica) | `docs/api/users/roles-permissions.mdx` |
   | Esquema de tabla o migración | `docs/services/**` o `docs/ml/overview.mdx` |
   | Variables de entorno o config Flyway | `docs/infrastructure/**` o `docs/config/**` |
   | Flujo de predicción/entrenamiento ML | `docs/ml/**` |
   | Rutas del gateway o nginx | `docs/infrastructure/docker-compose.mdx`, `docs/infrastructure/nginx.mdx` |
   | `default.conf` | también sincronizar `infra/nginx/sites-available/default.conf.template` (placeholders `${EC2_HOST}`, `${S3_WEBSITE_ENDPOINT}`) |

   Los cambios internos sin superficie pública (refactors, renombres privados, optimizaciones) **no** requieren docs — si todo el diff es interno, repórtalo y detente.

3. **Encuentra las páginas exactas**: busca en `docs/` los nombres concretos que cambiaron (ruta del endpoint, nombre del permiso, variable de entorno, tabla) con Grep. Revisa también `docs/docs.json` si hay que registrar páginas nuevas en la navegación.

4. **Actualiza los `.mdx`**: en español, respetando el estilo y los componentes Mintlify ya usados en páginas vecinas. Crea página nueva solo si el criterio de CLAUDE.md lo exige y ninguna existente cubre el tema (y agrégala a `docs/docs.json`).

5. **Verifica**: si `mint` está instalado, corre `cd docs && mint broken-links`. Reporta qué páginas se actualizaron y por qué, y cuáles cambios no requirieron docs.
