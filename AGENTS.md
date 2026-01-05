# Guía del Repositorio

Guía breve para mantener la documentación del sistema SGIVU sincronizada con los servicios y el orquestador.

## Estructura del Proyecto y Módulos

- `README.md`: índice general y guía rápida de componentes del sistema.
- `CONTRIBUTING.md`: ramas (`develop` por defecto), checklist de PR y Conventional Commits.
- `docs/diagrams/`: diagramas generales (01-system-architecture.puml, 02-build-pipeline.puml).
- `scripts/check-readme-boot-version.sh`: compara la versión de Spring Boot declarada en cada `README.md` vs `pom.xml` (sgivu-auth, sgivu-gateway, etc.).
- Guías específicas de cada servicio están en `apps/` e `infra/` (ej. `apps/backend/sgivu-auth/AGENTS.md`).

## Comandos de Build, Pruebas y Desarrollo

- Validar versiones documentadas: `./scripts/check-readme-boot-version.sh` (requiere Git y Perl).
- Render rápido de diagramas sin Java:

  ```bash
  docker run --rm -v "$(pwd)":/workspace ghcr.io/plantuml/plantuml -tsvg docs/diagrams/**/*.puml
  ```

- Render local con dependencias instaladas: `plantuml -tsvg docs/diagrams/**/*.puml`.
- Verifica conectividad hacia servicios en Compose usando los curls de `README.md`.

## Estilo de Código y Convenciones

- Código de servicios en inglés; documentación y comentarios en español.
- PlantUML: nombres descriptivos y numerados (`NN-tema.puml`); reutiliza estilos existentes y títulos breves.
- Shell scripts: `bash -euo pipefail`, indentación de 2 espacios y funciones cortas.

## Guía de Pruebas

- Si modificas versiones en READMEs de servicios, ejecuta `./scripts/check-readme-boot-version.sh` y atiende salidas `MISMATCH`.
- Abre `.puml` en VS Code con PlantUML Preview o genera SVGs; no subas artefactos renderizados adicionales (los PNG en `docs/diagrams/img` se mantienen como referencia).
- Al cambiar URLs o puertos, valida los smoke checks de `README.md` (gateway, config, discovery, auth).

## Commits y Pull Requests

- Commits en inglés usando Conventional Commits (`feat(auth): ...`, `docs: ...`) con alcance corto.
- Ramas desde `develop`: `feature/<descripcion-corta>` o `fix/<descripcion-corta>`; PRs contra `develop`.
- Cada PR debe incluir propósito, riesgos, comandos ejecutados y resultados; añade capturas solo si afectan UX de herramientas.
- Antes del review: ejecuta verificaciones relevantes, actualiza `README.md`/diagramas si cambian flujos o puertos y confirma que no se suben secretos ni `.env` reales.

## Seguridad y Configuración

- Usa `infra/compose/sgivu-docker-compose/.env.example` como base; nunca publiques credenciales ni endpoints internos sensibles.
- Para pruebas locales, mapea `sgivu-auth` en `/etc/hosts` en lugar de hardcodear URLs; en remoto, restringe el puerto 9000 a tu IP en el Security Group de EC2.
- Al usar el perfil `native` de `sgivu-config` en Docker, asegúrate de tener montado el volumen del repositorio de configuración en `/config-repo` (ver `docker-compose.dev.yml`).

## Notas Específicas del Servicio

- Renderiza diagramas con Docker para evitar dependencias locales cuando prepares documentación rápida.
