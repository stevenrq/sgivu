# sgivu-docker-compose - SGIVU

## Descripción

Repositorio de infraestructura para levantar el ecosistema de microservicios de SGIVU mediante Docker Compose. Define contenedores, redes, dependencias y credenciales compartidas para entornos locales y productivos.

## Arquitectura y Rol

- Orquesta los servicios de configuración, descubrimiento, gateway, autenticación, usuarios y clientes de SGIVU.
- Expone imágenes pre construidas (`stevenrq/*`) junto con las bases de datos y Redis.
- Punto de entrada para levantar el backend de forma integrada, reduciendo fricción entre desarrollo y operación.

## Tecnologías

- Docker, Docker Compose, Bash.

## Configuración

- `.env.dev`: URLs internas, credenciales locales de PostgreSQL y secretos compartidos. Usa `.env.dev.example` como plantilla y reemplaza placeholders.
- `.env`: orientado a despliegues en AWS (RDS, secretos, dominios). Usa `.env.example` como plantilla y reemplaza valores sensibles.

## Ejecución Local

Script unificado (levanta el stack completo usando imágenes ya construidas, sin reconstruir):

```bash
chmod +x run.sh
./run.sh --dev    # stack de desarrollo
./run.sh --prod   # stack productivo
```

Para detener la plataforma completa:

```bash
docker compose down
```

Agrega `-v` si necesitas limpiar los volúmenes creados durante las pruebas.

> Importante: `run.sh` y `run-service.sh` ya no usan `--build` por diseño.
> La construcción de imágenes es **explícita** vía `rebuild-service.sh` o
> `build-and-push-images.sh`. Ver "Construcción explícita" más abajo.

## Flujo de desarrollo localhost (recomendado)

Para trabajar día a día sin reconstruir imágenes en cada cambio, usa el modo
**híbrido**: solo la infra (Postgres, Redis, Config, Discovery) corre en
Docker; el microservicio que estás editando lo corres en el host. Spring
DevTools (ya incluido en todos los `pom.xml`) reinicia el contexto al guardar.

```text
+-----------------------------+        +-----------------------------+
|  Docker (dev-up.sh)         |        |  Host (mvn spring-boot:run) |
|                             |        |                             |
|  postgres   :5432           | <----- |  sgivu-user (en edición)    |
|  redis      :6379           | <----- |    DevTools restart < 5s    |
|  config     :8888           | <----- |                             |
|  discovery  :8761           | <----- |                             |
+-----------------------------+        +-----------------------------+
```

### Día a día (sin rebuild)

```bash
# 1. Levantar infra una sola vez (típicamente al inicio del día)
cd infra/compose/sgivu-docker-compose
chmod +x dev-up.sh host-run.sh   # solo la primera vez
./dev-up.sh                       # postgres, redis, config, discovery
./dev-up.sh --with sgivu-auth     # añade auth si NO lo estás editando

# 2a. Lanzar uno o varios servicios desde el directorio de compose
./host-run.sh sgivu-user                  # solo user
./host-run.sh sgivu-user sgivu-vehicle    # varios a la vez
./host-run.sh --all                       # todas las apps en host (auth, client,
                                          # user, vehicle, purchase-sale, gateway, ml)
./host-run.sh --status                    # ver qué hay corriendo
./host-run.sh --logs sgivu-user           # tail -f del log
./host-run.sh --stop                      # detener todos los host
./host-run.sh --stop sgivu-user           # detener uno

# 2b. (Alternativa) Desde el directorio del propio servicio
cd apps/backend/sgivu-user && ./run-host.sh
# Cada servicio tiene un run-host.sh que delega al host-run.sh central.
# Los flags pasan tal cual: ./run-host.sh --stop, ./run-host.sh --logs, etc.
```

> Equivalencia mental: `dev-up.sh` + `host-run.sh --all` cubre el mismo
> alcance que `run.sh --dev` (todo el stack arriba), pero las apps corren en
> host con hot-reload y sin rebuild. `run.sh --dev` sigue siendo útil para
> smoke-test del stack 100 % dockerizado.

`host-run.sh` carga `.env.dev`, sobreescribe los hosts a `localhost`, exporta
`SPRING_CONFIG_IMPORT` y `EUREKA_URL`, y lanza cada servicio con
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (Java) o
`uvicorn app.main:app --reload` (Python). Los logs y PIDs viven en `.host-run/`
(ignorado por git).

Apagar la infra cuando termines:

```bash
./dev-up.sh --down
```

### Alternativa: lanzar manualmente desde el IDE

Si prefieres correr el servicio desde tu IDE (IntelliJ, VS Code), define la
configuración de ejecución con perfil `dev` y exporta antes los env vars:

```bash
set -a; source infra/compose/sgivu-docker-compose/.env.dev; set +a
export SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
export EUREKA_URL=http://localhost:8761/eureka
export DEV_<SVC>_DB_HOST=localhost DEV_REDIS_HOST=localhost
```

Luego ejecuta desde el IDE en ese mismo entorno.

### Variables de entorno típicas al correr en host

Los YAML del config-repo ya usan `${VAR:default}` con `host.docker.internal`
o nombres Docker como default. Cuando corres en host, sobreescríbelos a
`localhost` (en `.env.dev` o exportándolos antes de `mvn`):

| Variable | Valor en host |
| --- | --- |
| `SPRING_CONFIG_IMPORT` | `configserver:http://localhost:8888` |
| `EUREKA_URL` | `http://localhost:8761/eureka` |
| `DEV_<SVC>_DB_HOST` | `localhost` |
| `DEV_REDIS_HOST` | `localhost` |
| `SGIVU_AUTH_URL` | `http://localhost:9000` (si auth está en host o expuesto) |
| `SGIVU_GATEWAY_URL` | `http://localhost:8080` (si gateway está en host) |

### Cuándo usar cada comando

| Comando | Cuándo |
| --- | --- |
| `./dev-up.sh` | Día a día. Levanta infra; tú corres el servicio editado en host. |
| `./dev-up.sh --with <svc>` | Cuando necesitas un servicio extra dockerizado. |
| `./run.sh --dev` | Smoke test del stack completo en Docker (imágenes existentes). |
| `./run-service.sh --dev <svc>` | Recrear un contenedor sin reconstruir. |
| `./rebuild-service.sh --dev <svc>` | Reconstrucción explícita (mvn + docker build) de un servicio. Antes de PR. |
| `./rebuild-service.sh --dev <svc> --push` | Igual, y publica a Docker Hub. |
| `./build-and-push-images.sh` | Reconstruye y publica TODAS las imágenes. Releases. |

## Construcción explícita

`docker compose up` **nunca** reconstruye imágenes en este proyecto: los
archivos compose declaran solo `image:`, sin `build:`. Las únicas vías de
construcción son:

- `./rebuild-service.sh --dev <servicio>` — reconstruye un servicio puntual
  (mvn package + docker build) y recrea solo su contenedor.
- `./build-and-push-images.sh` — recorre todos los servicios, los compila,
  construye sus imágenes y publica a Docker Hub.

Esta separación es por diseño: el ciclo de cambio diario no debe pagar el
costo de un build de Docker.

### Riesgos a recordar

- **OAuth2 issuer matching (importante):** `sgivu-auth` reporta su issuer
  según el hostname con el que se accede (Docker: `http://sgivu-auth:9000`;
  host: `http://localhost:9000`). Los resource servers
  (`sgivu-user`, `sgivu-vehicle`, `sgivu-purchase-sale`, `sgivu-gateway`)
  validan que el `iss` del token coincida con el `issuer-uri` configurado.
  Por eso **auth y los resource servers deben correr en el mismo entorno**:
  todos en host (`./host-run.sh sgivu-auth sgivu-user ...`) o todos en
  Docker (`./dev-up.sh --with sgivu-auth --with sgivu-user ...`). Mezclar
  rompe el bootstrap del `JwtDecoder`.
- **Divergencia dev vs prod:** correr en host puede ocultar problemas de
  contenedor (timezone, locale, paths). Antes de cada PR: `./rebuild-service.sh
  --dev <svc>` y validar con `docker compose logs`.
- **Conflicto de puertos:** no levantes con `--with sgivu-X` el mismo
  servicio que estás corriendo en host (pelean el puerto).
- **Eureka desde host:** otros servicios dentro de Docker pueden no resolver
  el hostname con el que tu app en host se registra. Si el gateway corre en
  Docker y necesita rutear a tu servicio en host, exporta
  `EUREKA_INSTANCE_HOSTNAME=host.docker.internal` y
  `EUREKA_INSTANCE_PREFER_IP_ADDRESS=false`.
- **Refresh del Config Server:** un cambio en un YAML del `sgivu-config-repo`
  no llega a las apps automáticamente. Usa `POST /actuator/refresh` o
  reinicia el servicio.

## Ejecutar servicios individualmente

Para levantar uno o varios servicios de forma independiente (útil para desarrollo o debugging), use el script `run-service.sh`. El script selecciona el archivo de compose y el archivo de variables de entorno según la bandera `--dev|--prod`. A diferencia de `run.sh` (que arranca todo el stack), `run-service.sh` sólo crea/reinicia los servicios que usted indique.

```bash
# Hacer el script ejecutable (una vez)
chmod +x run-service.sh

# Levantar uno o varios servicios en modo desarrollo (usa docker-compose.dev.yml y .env.dev)
./run-service.sh --dev sgivu-auth sgivu-user

# Levantar servicios en modo producción (usa docker-compose.yml y .env)
./run-service.sh --prod sgivu-gateway sgivu-vehicle
```

## Respaldos automáticos

Cuando necesite generar copias de seguridad de las bases de datos y de Redis, use `dbs-backups.sh`. El script toma el archivo de entorno según `--dev|--prod`, respalda PostgreSQL y Redis por separado, y elimina archivos antiguos según la retención configurada.

```bash
# Hacer el script ejecutable (una vez)
chmod +x dbs-backups.sh

# Respaldo en modo desarrollo con retención por defecto de 7 días
./dbs-backups.sh --dev

# Respaldo en modo producción con retención personalizada
./dbs-backups.sh --prod --retain 14

# Sobrescribir el directorio destino de backups
./dbs-backups.sh --prod --backup-root /mnt/backups/sgivu
```

### Ejemplo: programar con cron + flock

Usa `flock` para evitar que varias ejecuciones se solapen si un backup tarda más que el intervalo. Siempre usa rutas absolutas y redirige la salida a un logfile para facilitar la depuración.

Ejemplo (añadir con `crontab -e`):

```bash
# Ejecuta cada minuto; evita solapamientos con `flock` y guarda logs
* * * * * /usr/bin/flock -n /tmp/sgivu-dbs-backups.lock /home/steven/Documents/github/sgivu/infra/compose/sgivu-docker-compose/dbs-backups.sh --dev >> /home/steven/Backups/sgivu-dbs-backups/cron.log 2>&1
```

Notas:

- Si el usuario del cron necesita permisos para usar Docker, agréguelo al grupo `docker` (`sudo usermod -aG docker $USER`) o ponga la entrada en el crontab de `root` (`sudo crontab -e`).
- Programar backups cada minuto suele ser excesivo: ajusta el intervalo según la duración del backup y la carga del sistema.

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

- `./build-and-push-images.sh` construye y publica todas las imágenes `stevenrq/*` recorriendo los servicios hermanos.
- Invoca `build-image.sh` cuando existe o compila con Maven en proyectos Java.
- `./rebuild-service.sh --dev sgivu-auth` reconstruye la imagen localmente y recrea solo ese contenedor en el stack. Añada `--push` para publicar en Docker Hub (`./rebuild-service.sh --dev --push sgivu-auth`).

## Despliegue

- Ajusta `docker-compose.yml` para recursos administrados (RDS, S3) usando `.env`.
- Usa `clave.pem` con permisos `chmod 400` para acceso a EC2.
- Exponer solo puertos requeridos; preferir Gateway detrás de Load Balancer.

## Monitoreo

- Usa `docker compose ps` y `docker compose logs -f sgivu-gateway` para verificar arranques.

## Solución de Problemas

- Variables faltantes en `docker compose config`: revisa `.env.dev`/`.env` usando `.env.example` como base.
- Puertos ocupados (8000/8080/8081/8082/8083/8084/9000/8888/8761): libera procesos o ajusta mapeos.
- Dependencias no listas: espera `sgivu-config`/`sgivu-discovery` antes de `auth`/`gateway`.
- Sin acceso a Config Server: revisa `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y la etiqueta en Compose. Si usas el perfil `native`, valida el montaje del volumen `/config-repo`.
- Acceso desde host (desarrollo local): agrega alias en `/etc/hosts` para `sgivu-auth` y `sgivu-gateway`. En producción con Nginx no es necesario.
  Ver `apps/backend/sgivu-auth/sgivu-auth-access.md` y `apps/backend/sgivu-gateway/sgivu-gateway-access.md`.

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
