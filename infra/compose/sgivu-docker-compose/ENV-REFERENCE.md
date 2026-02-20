# Referencia de variables de entorno

Este documento explica **por qué** existe cada variable, qué se rompe si está mal configurada y las dependencias ocultas entre ellas. Los archivos `.env` / `.env.dev` solo contienen comentarios breves; aquí está el detalle completo.

## Flujo general

```text
.env / .env.dev → docker-compose → contenedores
                                       ↓
                              variables de entorno
                                       ↓
                          Config Server (YAMLs con ${VAR})
                                       ↓
                    Spring Boot @Value / Pydantic Settings
```

`SPRING_PROFILES_ACTIVE` es el interruptor maestro: selecciona `{servicio}-dev.yml` o `{servicio}-prod.yml` en el Config Server, que a su vez referencian las variables `DEV_*` o `PROD_*`.

---

## Variables críticas y sus dependencias

### `SGIVU_AUTH_URL` ↔ `ISSUER_URL`

Estas dos variables **deben coincidir exactamente**:

- `ISSUER_URL` → `sgivu-auth` la escribe en el claim `iss` de cada JWT (`SecurityConfig.java` → `AuthorizationServerSettings`).
- `SGIVU_AUTH_URL` → cada resource server la usa como `issuer-uri` para validar ese claim `iss`.

Si difieren, **todos los JWT son rechazados** → fallo total de autenticación.

En **producción**, ambas apuntan al hostname público de EC2 (ej: `http://ec2-...amazonaws.com`). Los contenedores resuelven este hostname vía `extra_hosts` en `docker-compose.yml` → `host-gateway` → Nginx.

En **desarrollo**, solo se define `SGIVU_AUTH_URL=http://sgivu-auth:9000`. `ISSUER_URL` no se necesita porque los YAMLs base ya definen ese mismo default.

### `SGIVU_GATEWAY_SECRET`

Secreto OAuth2 compartido entre `sgivu-auth` (proveedor) y `sgivu-gateway` (cliente). `sgivu-auth` lo almacena **hasheado con BCrypt** en la BD al arrancar (`ClientRegistrationRunner`).

**Si se cambia después del primer arranque**, hay que eliminar el registro del cliente en `sgivu_auth_db` para que se re-cree con el nuevo hash. De lo contrario, el intercambio de `authorization_code` por tokens falla silenciosamente.

### `SERVICE_INTERNAL_SECRET_KEY`

Secreto compartido para el header `X-Internal-Service-Key` en llamadas service-to-service sin JWT. Lo usan **los 7 servicios backend + sgivu-ml**. Si difiere en un solo servicio, sus endpoints internos devuelven 401/403.

Ejemplos de flujos que dependen de esta clave:

- `sgivu-auth` → `sgivu-user`: validación de credenciales en login.
- `sgivu-ml` → `sgivu-purchase-sale`: obtención de contratos para entrenamiento.
- `sgivu-ml` → `sgivu-vehicle`: obtención de datos de vehículos.

### `REDIS_PASSWORD`

Se usa en **dos lugares distintos** que deben coincidir:

1. Comando `--requirepass` del contenedor `sgivu-redis`.
2. `spring.data.redis.password` en la configuración del gateway.

Si difieren, el gateway no puede conectarse a Redis → todas las sesiones HTTP fallan → no hay autenticación.

Redis se usa **exclusivamente** por `sgivu-gateway` para persistir sesiones (patrón BFF). Ningún otro servicio lo usa.

### `PROD_ANGULAR_APP_URL` / `DEV_ANGULAR_APP_URL`

Controla, desde `sgivu-auth` y `sgivu-gateway`:

- CORS (`SecurityConfig` de auth y gateway).
- Redirect tras login/logout OAuth2 (`LoginController`, `SsoLogoutController`).
- `postLogoutRedirectUri` del cliente OAuth2 (`ClientRegistrationRunner`).

Debe coincidir con el dominio real del frontend. Si no: CORS bloquea peticiones y los redirects OAuth2 van a URLs incorrectas.

Relación con `AWS_S3_ALLOWED_ORIGINS`: deben incluir los mismos dominios para que el navegador cargue imágenes desde URLs prefirmadas de S3 sin errores CORS.

---

## Tabla de variables

### Perfil

| Variable                 | Consumida por       | Por qué existe                                                                                                                        |
| ------------------------ | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | Todos los servicios | Selecciona overlay YAML (`dev`/`prod`) del Config Server. `sgivu-config` lo ignora; usa `native`/`git` hardcodeado en docker-compose. |

### URLs de servicios

| Variable                  | Consumida por                     | Por qué existe                                                                                                                             |
| ------------------------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `EUREKA_URL`              | Todos los servicios               | Si es incorrecta, ningún servicio se registra y el gateway devuelve 503 en todas las rutas API (`lb://` no puede resolver).                |
| `SGIVU_AUTH_URL`          | Resource servers + gateway        | Valida el claim `iss` de los JWT. Ver sección de dependencias arriba.                                                                      |
| `ISSUER_URL`              | `sgivu-auth`                      | Define el `iss` de los JWT emitidos. Solo en prod (dev usa default). Ver sección de dependencias arriba.                                   |
| `SGIVU_USER_URL`          | `sgivu-auth`                      | `sgivu-auth` llama a `sgivu-user` para validar credenciales en login (`CredentialsValidationService`).                                     |
| `SGIVU_CLIENT_URL`        | `sgivu-purchase-sale`             | Obtiene datos de clientes al crear transacciones.                                                                                          |
| `SGIVU_VEHICLE_URL`       | `sgivu-purchase-sale`, `sgivu-ml` | Datos de vehículos para transacciones y entrenamiento ML.                                                                                  |
| `SGIVU_PURCHASE_SALE_URL` | `sgivu-ml`                        | Obtiene contratos para entrenar el modelo de predicción.                                                                                   |
| `SGIVU_GATEWAY_URL`       | `sgivu-auth`                      | Registra la redirect URI OAuth2 del gateway (`/login/oauth2/code/sgivu-gateway`). Si no coincide con la URL real: `redirect_uri mismatch`. |

> **Nota:** estas URLs se usan en llamadas directas service-to-service vía `RestClient`/`httpx`. El gateway **no** las usa; resuelve servicios por nombre Eureka (`lb://`).

### JWT

| Variable                | Consumida por                 | Por qué existe                                                                       |
| ----------------------- | ----------------------------- | ------------------------------------------------------------------------------------ |
| `JWT_KEYSTORE_LOCATION` | `sgivu-auth`                  | Ruta al keystore JKS con la clave RSA para firmar JWT. Si es incorrecto, no arranca. |
| `JWT_KEYSTORE_PASSWORD` | `sgivu-auth`                  | Contraseña para abrir el keystore. Si es incorrecta, crash al arrancar.              |
| `JWT_KEY_ALIAS`         | `sgivu-auth`                  | Alias de la clave RSA dentro del keystore. Si no existe, crash al arrancar.          |
| `JWT_KEY_PASSWORD`      | `sgivu-auth`                  | Contraseña de la clave privada RSA. Si es incorrecta, crash al arrancar.             |
| `SGIVU_GATEWAY_SECRET`  | `sgivu-auth`, `sgivu-gateway` | Ver sección de dependencias arriba.                                                  |

### Redis

| Variable         | Consumida por                  | Por qué existe                      |
| ---------------- | ------------------------------ | ----------------------------------- |
| `REDIS_HOST`     | `sgivu-gateway`                | Host del contenedor Redis.          |
| `REDIS_PORT`     | `sgivu-gateway`                | Puerto de Redis.                    |
| `REDIS_PASSWORD` | `sgivu-gateway`, `sgivu-redis` | Ver sección de dependencias arriba. |

### AWS / S3

| Variable                 | Consumida por   | Por qué existe                                                                                                |
| ------------------------ | --------------- | ------------------------------------------------------------------------------------------------------------- |
| `AWS_ACCESS_KEY`         | `sgivu-vehicle` | Credenciales para `S3Client`/`S3Presigner`. Si son inválidas, operaciones S3 fallan con 403.                  |
| `AWS_SECRET_KEY`         | `sgivu-vehicle` | Idem.                                                                                                         |
| `AWS_REGION`             | `sgivu-vehicle` | Región del bucket S3.                                                                                         |
| `AWS_VEHICLES_BUCKET`    | `sgivu-vehicle` | Nombre del bucket donde se almacenan imágenes de vehículos.                                                   |
| `AWS_S3_ALLOWED_ORIGINS` | `sgivu-vehicle` | Orígenes CORS del bucket S3 (`S3BucketCorsConfig`). Debe incluir los mismos dominios que `*_ANGULAR_APP_URL`. |

### PostgreSQL

| Variable                         | Consumida por               | Por qué existe                                                                                           |
| -------------------------------- | --------------------------- | -------------------------------------------------------------------------------------------------------- |
| `POSTGRES_HOST/DB/USER/PASSWORD` | Contenedor `sgivu-postgres` | Inicializa la instancia PostgreSQL. Todos los servicios comparten esta instancia pero con BDs separadas. |
| `{PROD\                          | DEV}_{SERVICE}_DB_*`        | Cada servicio                                                                                            |

### Flyway

| Variable                     | Consumida por              | Por qué existe                                                                                                                                                                                        |
| ---------------------------- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `FLYWAY_BASELINE_ON_MIGRATE` | Todos los servicios con BD | `false` en prod: exige historial existente, evita baseline silencioso. `true` en dev: permite BDs preexistentes sin historial. **Nota:** los YAMLs de dev hardcodean `true`, ignorando esta variable. |

### MySQL + Zipkin

| Variable                                          | Consumida por             | Por qué existe                                                                                                                                  |
| ------------------------------------------------- | ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `MYSQL_HOST/ROOT_PASSWORD/DATABASE/PASSWORD/USER` | Contenedor `sgivu-mysql`  | Inicializan MySQL (imagen Docker estándar). MySQL se usa **exclusivamente** para Zipkin.                                                        |
| `STORAGE_TYPE`, `MYSQL_DB`, `MYSQL_PASS`          | Contenedor `sgivu-zipkin` | La imagen de Zipkin usa **nombres distintos** a los de MySQL para los mismos valores. `MYSQL_HOST` y `MYSQL_USER` los comparten ambas imágenes. |

### OpenAPI

| Variable             | Consumida por                 | Por qué existe                                                                                                                       |
| -------------------- | ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `OPENAPI_SERVER_URL` | Todos los backend (solo prod) | Agrega el hostname público en Swagger UI para que "Try it out" apunte a la URL correcta. En dev no se define; Swagger usa localhost. |

### sgivu-ml (FastAPI)

Estas variables las consume Pydantic `Settings` directamente (**no** pasan por Config Server).

| Variable                             | Por qué existe                                                                                                                              |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `ENVIRONMENT`                        | Selecciona el prefijo de BD (`dev_ml_db_*` / `prod_ml_db_*`).                                                                               |
| `APP_NAME`, `APP_VERSION`            | Metadatos de la app FastAPI (título, versión).                                                                                              |
| `SGIVU_AUTH_DISCOVERY_URL`           | Endpoint OIDC para descubrir JWKS y validar JWT. El `issuer` descubierto debe coincidir con `ISSUER_URL`; si difieren, la validación falla. |
| `MODEL_DIR`, `MODEL_NAME`            | Ruta y nombre del modelo serializado.                                                                                                       |
| `REQUEST_TIMEOUT_SECONDS`            | Timeout para llamadas HTTP a otros servicios.                                                                                               |
| `DEFAULT_HORIZON_MONTHS`             | Horizonte de predicción por defecto.                                                                                                        |
| `MIN_HISTORY_MONTHS`                 | Mínimo de historial requerido para predecir.                                                                                                |
| `TARGET_COLUMN`                      | Columna objetivo en los datos de entrenamiento.                                                                                             |
| `RETRAIN_CRON`, `RETRAIN_TIMEZONE`   | Programación de reentrenamiento automático.                                                                                                 |
| `PERMISSIONS_PREDICT/RETRAIN/MODELS` | Permisos OAuth2 por endpoint. Si se dejan vacíos, solo exige JWT válido sin verificar permisos.                                             |
| `DATABASE_ENV`, `{DEV\               | PROD}_ML_DB_*`                                                                                                                              |
| `DATABASE_AUTO_CREATE`               | `true` crea tablas al arrancar. Peligroso en prod.                                                                                          |
| `DATABASE_ECHO`                      | `true` activa logging SQL de SQLAlchemy. Ruidoso en prod.                                                                                   |
