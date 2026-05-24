# sgivu-auth - SGIVU

## Descripción

**`sgivu-auth`** es el Authorization Server (OpenID Connect / OAuth2) del ecosistema **SGIVU**. Emite JWT firmados con un keystore JKS, gestiona clientes OAuth2, autoriza usuarios y persiste autorizaciones y sesiones en PostgreSQL.

## Tecnologías y Dependencias

- Java 25
- Spring Boot 4.0.1, Spring Cloud 2025.1.0
- Spring Authorization Server (OAuth2 / OIDC), Spring Security
- Spring MVC (servlet) + Thymeleaf (página de login)
- Spring Cloud Config (client), Eureka client
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Session (JDBC) — sesiones persistidas en `SPRING_SESSION`
- Spring Boot Actuator (`health`, `info`)
- SpringDoc OpenAPI 3.0.1 (Swagger)
- Spring Cloud Circuit Breaker (Resilience4j)

## Requisitos Previos

- JDK 25
- Maven 3.9+
- Docker & docker-compose
- PostgreSQL
- `sgivu-config` y `sgivu-discovery` disponibles (o arrancados via docker-compose)

## Arranque y Ejecución

### Desarrollo (docker-compose)

1. Arrancar infra (desde `infra/compose/sgivu-docker-compose`):

    ```bash
    docker compose -f docker-compose.dev.yml up -d
    ```

2. Compilar y ejecutar la app (opción local):

    ```bash
    ./mvnw clean package
    ./mvnw spring-boot:run
    ```

### Docker

```bash
./build-image.sh          # construye localmente
./build-image.sh --push   # construye y publica en Docker Hub

docker build -t stevenrq/sgivu-auth:0.1.0 .
docker run --env-file infra/compose/sgivu-docker-compose/.env -p 9000:9000 stevenrq/sgivu-auth:0.1.0
```

### Producción

En producción el acceso pasa normalmente por Nginx en EC2; Nginx rutea `/oauth2/*`, `/login`, `/.well-known/*` al contenedor `sgivu-auth` (puerto interno 9000). Configurar `ISSUER_URL` para que coincida con el hostname expuesto.

## Endpoints Principales

### OIDC / OAuth2 (built-in)

| Endpoint | Descripción |
| --- | --- |
| `/.well-known/openid-configuration` | Metadatos del issuer OIDC |
| `/oauth2/authorize` | Endpoint de autorización (Authorization Code + PKCE) |
| `/oauth2/token` | Endpoint de token (authorization_code, refresh_token) |
| `/oauth2/revoke` | Revocación de tokens |
| `/oauth2/introspect` | Introspección de tokens |
| `/oauth2/jwks` | JWKS para verificación de tokens |

### Endpoints custom (controllers)

| Endpoint | Descripción |
| --- | --- |
| `GET /` y `GET /login` | Redirección y formulario de login (Thymeleaf) |
| `POST /api/validate-credentials` | Validación de credenciales en tiempo real (formulario de login) |
| `GET /oauth2/consent` | Pantalla de consentimiento de scopes |
| `GET /sso-logout` | Inicio de logout SSO |
| `GET /error` | Página de error custom |

### Scopes soportados

`openid`, `profile`, `email`, `phone`, `address`, `offline_access`, `api`, `read`, `write`.

## Seguridad

- **Keystore / JWT:** La clave para firmar JWT se carga desde `sgivu.jwt.keystore.location` y `sgivu.jwt.keystore.password`. No incluir `keystore.jks` en el repo (está en `.gitignore`); debe proveerse desde un secret manager o pipeline.
- **Clientes por defecto:**
  - `sgivu-gateway` — registrado siempre por `ClientRegistrationRunner`. Su secret se inyecta vía `${SGIVU_GATEWAY_SECRET}` y se almacena hasheado con BCrypt en la tabla `clients`.
  - `postman-client` y `oauth2-debugger-client` — registrados solo en perfil `dev` por `DevClientsRegistrationRunner` para pruebas y depuración.
  - Si cambia el secret tras el primer arranque, es necesario eliminar la fila correspondiente en `clients` para que el runner vuelva a sembrar la entrada.
- **Validación de credenciales:** `CredentialsValidationService` realiza una llamada interna a `sgivu-user` (`GET /v1/users/username/{username}`) usando el header `X-Internal-Service-Key`.
- **Sesiones:** persistidas en JDBC (tabla `SPRING_SESSION`) — diferentes a las sesiones BFF del gateway, que se persisten en Redis.

## Migraciones

- Flyway está habilitado y las migraciones se encuentran en `src/main/resources/db/migration`.
- `V1__initial_schema.sql` crea las tablas `clients`, `authorizations`, `authorization_consents` y `SPRING_SESSION` (usada por Spring Session JDBC).
- Propiedades de datasource y Flyway se definen en `sgivu-config-repo/sgivu-auth.yml`, `sgivu-auth-dev.yml`, `sgivu-auth-prod.yml`.

## Observabilidad

- **Actuator:** health/info (exposición depende del profile: dev expone más endpoints).

## Pruebas

```bash
./mvnw test
```

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| Issuer mismatch | Verificar `ISSUER_URL` vs URL real usada por el navegador/Nginx |
| Keystore missing | Asegurar `keystore.jks` disponible en runtime o proveer `sgivu.jwt.keystore.location` correcto |
| Servicio de usuarios inaccesible | `CredentialsValidationService` fallará; revisar red y `SERVICE_INTERNAL_SECRET_KEY` |

## Contribuciones

1. Fork → branch → PR
2. Para cambios funcionales abrir PR con tests
