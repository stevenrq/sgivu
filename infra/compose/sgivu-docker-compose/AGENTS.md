# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `docker-compose.yml`: stack productivo con imágenes `stevenrq/*`.
- `docker-compose.dev.yml`: stack local con MySQL/PostgreSQL incluidos y los mismos servicios SGIVU.
- `.env.example` y `.env.dev.example`: plantillas; mantén tus `.env*` sin versionar.
- Scripts clave: `run.bash` (entrada unificada), `build-and-push-images.bash` (recorre microservicios en `../` y construye/pusha), `rebuild-service.bash` (reconstruye/publica un servicio y recrea el contenedor en Compose), `copy-from-local-to-remote.sh` (rsync a EC2), `remove-containers-images.sh` (limpieza agresiva).
- Documentos: diagramas PlantUML en `../../../docs/diagrams/*.puml`.

## Comandos de Build, Pruebas y Desarrollo

- `./run.bash --dev`: levanta stack local con `.env.dev`.
- `./run.bash --prod`: levanta stack productivo leyendo `.env`.
- `docker compose -f docker-compose.dev.yml --env-file .env.dev config`: valida sintaxis y variables.
- `docker compose -f docker-compose.dev.yml down -v`: detiene y borra volúmenes locales.
- `./build-and-push-images.bash`: construye/publica `stevenrq/<servicio>:v1`; ajusta `SERVICES` y tags si versionas.
- `./rebuild-service.bash --dev sgivu-auth`: reconstruye/publica una imagen y recrea solo ese contenedor en Compose.
- Monitoreo rápido: `docker compose ps` y `docker logs -f sgivu-gateway` para ver arranque.

## Estilo de Código y Convenciones

- YAML con 2 espacios; sincroniza `service`/`container_name` con la imagen y puertos.
- Variables en MAYÚSCULAS snake_case; reutiliza URLs/base paths de las plantillas para evitar desalineaciones.
- Scripts Bash con `set -euo pipefail`; código y commits en inglés, documentación en español; nunca incluyas secretos en scripts o Compose.

## Guía de Pruebas

- Smoke test: `docker compose -f docker-compose.dev.yml --env-file .env.dev up -d --build`, espera `sgivu-config`/`sgivu-discovery` y verifica `http://localhost:8080/actuator/health` vía gateway.
- Si tocas el pipeline de build, prueba un servicio a la vez ajustando temporalmente `SERVICES` y confirma el push en el registry antes de mover tags compartidos.
- Usa `docker compose logs --tail=50 sgivu-auth` o `sgivu-user` para validar nuevas variables o dependencias.

## Commits y Pull Requests

- Commits con Conventional Commits (`feat:`, `fix:`, `chore:`); sujetos cortos en inglés.
- PRs: explica propósito, servicios tocados, variables nuevas/renombradas y si actualizaste diagramas o docs; adjunta logs/capturas si algún contenedor sigue inestable.
- No subas `.env*`, `sgivu-ec2-keypair.pem` ni endpoints sensibles; revisa `.gitignore` antes de hacer push.

## Seguridad y Configuración

- Parte de `.env.dev.example`/`.env.example`, cambia placeholders y comparte solo plantillas.
- Al agregar servicios, revisa `depends_on` para asegurar orden de arranque y evitar timeouts.
- Antes de `copy-from-local-to-remote.sh`, usa `chmod 400 sgivu-ec2-keypair.pem` y, si dudas, `rsync --dry-run`.
- Actualiza `../../../docs/diagrams/*.puml` cuando cambie la topología del stack para mantener la documentación alineada.

## Notas Específicas del Servicio

- Usa `docker compose ps` y `docker logs -f sgivu-gateway` para diagnósticos rápidos al levantar el stack.
