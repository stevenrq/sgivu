# sgivu-docker-compose - SGIVU

## Descripción

Repositorio de infraestructura para levantar el ecosistema de microservicios de SGIVU mediante Docker Compose. Define contenedores, redes, dependencias y credenciales compartidas para entornos locales y productivos.

## Arquitectura y Rol

- Orquesta los servicios de configuración, descubrimiento, gateway, autenticación, usuarios y clientes de SGIVU.
- Expone imágenes pre construidas (`stevenrq/*`) y servicios de terceros como Zipkin y bases de datos.
- Punto de entrada para levantar el backend de forma integrada, reduciendo fricción entre desarrollo y operación.

## Tecnologías

- Docker, Docker Compose, Bash.

## Configuración

- `.env.dev`: URLs internas, credenciales locales de MySQL/PostgreSQL y secretos compartidos. Usa `.env.dev.example` como plantilla y reemplaza placeholders.
- `.env`: orientado a despliegues en AWS (RDS, secretos, dominios). Usa `.env.example` como plantilla y reemplaza valores sensibles.

## Ejecución Local

Script unificado:

```bash
chmod +x run.bash
./run.bash --dev    # stack de desarrollo
./run.bash --prod   # stack productivo
```

Para detener la plataforma completa:

```bash
docker compose down
```

Agrega `-v` si necesitas limpiar los volúmenes creados durante las pruebas.

## Endpoints Principales

Puertos típicos en local (según `docker-compose*.yml`):

- Gateway: `http://localhost:8080`
- Auth: `http://localhost:9000`
- Config: `http://localhost:8888`
- Discovery: `http://localhost:8761`
- User: `http://localhost:8081`
- Client: `http://localhost:8082`
- Vehicle: `http://localhost:8083`
- Purchase-sale: `http://localhost:8084`
- ML: `http://localhost:8000`
- Zipkin (opcional): `http://localhost:9411`

## Seguridad

- No versionar secretos reales en `.env*`; usa placeholders y gestores de secretos.
- Revisa `depends_on` al agregar servicios para garantizar el orden de arranque.
- Ajusta reglas de red y puertos expuestos únicamente a lo necesario.

## Dependencias

- `sgivu-config` publica configuración para el resto de microservicios.
- `sgivu-config-repo` provee los YAML consumidos por `sgivu-config`.
- `sgivu-auth`, `sgivu-user`, `sgivu-client`, `sgivu-gateway` se despliegan aquí mediante imágenes pre construidas.

## Dockerización

- Orquestación completa via `docker compose` con variantes `docker-compose.yml` y `docker-compose.dev.yml`.

## Build y Push Docker

- `./build-and-push-images.bash` construye y publica todas las imágenes `stevenrq/*` recorriendo los servicios hermanos.
- Invoca `build-image.bash` cuando existe o compila con Maven en proyectos Java.
- `./rebuild-service.bash --dev sgivu-auth` reconstruye/publica la imagen y recrea solo ese contenedor en el stack.

## Despliegue

- Ajusta `docker-compose.yml` para recursos administrados (RDS, S3) usando `.env`.
- Usa `clave.pem` con permisos `chmod 400` para acceso a EC2.
- Exponer solo puertos requeridos; preferir Gateway detrás de Load Balancer.

## Monitoreo

- Usa `docker compose ps` y `docker compose logs -f sgivu-gateway` para verificar arranques.
- Zipkin disponible si se configura `ZIPKIN_BASE_URL`.

## Solución de Problemas

- Variables faltantes en `docker compose config`: revisa `.env.dev`/`.env` usando `.env.example` como base.
- Puertos ocupados (8000/8080/8081/8082/8083/8084/9000/8888/8761/9411): libera procesos o ajusta mapeos.
- Dependencias no listas: espera `sgivu-config`/`sgivu-discovery` antes de `auth`/`gateway`.
- Sin trazas en Zipkin: confirma `ZIPKIN_BASE_URL` y `management.tracing` habilitado.
- Sin acceso a Config Server: revisa `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y la etiqueta en Compose. Si usas el perfil `native`, valida el montaje del volumen `/config-repo`.
- Acceso desde host (desarrollo local): agrega alias en `/etc/hosts` para `sgivu-auth` y `sgivu-gateway`. En producción con Nginx no es necesario.
  Ver `apps/backend/sgivu-auth/sgivu-auth-access.md` y `apps/backend/sgivu-gateway/sgivu-gateway-access.md`.

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
