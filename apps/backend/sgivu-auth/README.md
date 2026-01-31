# sgivu-auth - SGIVU

## Descripción

**`sgivu-auth`** es el Authorization Server (OpenID Connect / OAuth2) del ecosistema **SGIVU**. Emite JWT firmados con un keystore JKS, gestiona clientes OAuth2, autoriza usuarios y persiste autorizaciones y sesiones en PostgreSQL.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 4.0.1
- Spring Authorization Server (OAuth2 / OIDC), Spring Security
- Spring Cloud Config (client), Eureka client
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Session (JDBC)
- Spring Boot Actuator, Micrometer Tracing, Zipkin
- SpringDoc OpenAPI (Swagger)

## Requisitos Previos

- JDK 21
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
./build-image.bash
docker build -t stevenrq/sgivu-auth:v1 .
docker run --env-file infra/compose/sgivu-docker-compose/.env -p 9000:9000 stevenrq/sgivu-auth:v1
```

### Producción

En producción el acceso pasa normalmente por Nginx en EC2; Nginx rutea `/oauth2/*`, `/login`, `/.well-known/*` al contenedor `sgivu-auth` (puerto interno 9000). Configurar `ISSUER_URL` para que coincida con el hostname expuesto.

## Endpoints Principales

| Endpoint | Descripción |
| --- | --- |
| `/.well-known/openid-configuration` | Metadatos del issuer OIDC |
| `/oauth2/authorize` | Endpoint de autorización OAuth2 |
| `/oauth2/token` | Endpoint de token OAuth2 |
| `/oauth2/jwks` | JWKS para verificación de tokens |
| `/login` | Página de login |

## Seguridad

- **Keystore / JWT:** La clave para firmar JWT se carga desde `sgivu.jwt.keystore.location` y `sgivu.jwt.keystore.password`. No incluir `keystore.jks` en el repo (está en `.gitignore`); debe proveerse desde un secret manager o pipeline.
- **Clientes por defecto:** En arranque `ClientRegistrationRunner` registra: `sgivu-gateway` (usa `gateway-client.secret`), `postman-client` (secret `postman-secret`), `oauth2-debugger-client` (secret `oauth2-debugger-secret`). Los secrets por defecto **no** son seguros para producción.

## Migraciones

- Flyway está habilitado y las migraciones se encuentran en `src/main/resources/db/migration`.
- `V1__initial_schema.sql` crea tablas `clients`, `authorizations`, `authorization_consents` y `SPRING_SESSION` (usada por Spring Session JDBC).
- Propiedades de datasource y Flyway se definen en `sgivu-config-repo/sgivu-auth-*.yml`.

## Observabilidad

- **Actuator:** health/info (exposición depende del profile: dev expone más endpoints).
- **Tracing:** Zipkin endpoint configurado (`http://sgivu-zipkin:9411/api/v2/spans`). Hay spans en servicios clave (`CredentialsValidationService`, `JpaUserDetailsService`).

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
