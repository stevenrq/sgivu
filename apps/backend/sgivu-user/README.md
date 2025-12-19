# SGIVU - sgivu-user

## Descripción

Microservicio para administrar el ciclo de vida de usuarios: creación, lectura, actualización, desactivación y asignación de roles/permisos. Expone APIs REST para el gateway y otros servicios internos.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud.
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`.
- APIs RESTful para frontend y servicios (roles, permisos y usuarios); se registra en Eureka y se balancea vía gateway.
- Configuración centralizada (datasource, JWT, Zipkin) desde Config Server; persistencia en PostgreSQL.

## Tecnologías

- Lenguaje: Java 21 (Amazon Corretto)
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Seguridad: OAuth 2.1 Resource Server, JWT, autorización granular por roles/permisos
- Persistencia: Spring Data JPA, PostgreSQL, scripts `schema.sql`/`data.sql`
- Observabilidad: Actuator, Micrometer Tracing + Zipkin
- Utilitarios: MapStruct, Lombok, Validation API

## Configuración

- Variables clave: `SPRING_CONFIG_IMPORT`, `SPRING_PROFILES_ACTIVE`, `SERVICE_INTERNAL_SECRET_KEY`, `services.sgivu-auth.url`, propiedades de datasource.
- `application-local.yml` recomendado para desarrollo si no se usa Config Server.

## Ejecución Local

```bash
./mvnw clean package
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Requiere Config Server, Eureka, PostgreSQL y opcionalmente Zipkin. Endpoints accesibles vía gateway en `http://localhost:8080`.

## Endpoints Principales

```text
POST   /v1/users
GET    /v1/users/{id}
GET    /v1/users/username/{user}
GET    /v1/users
GET    /v1/users/page/{page}
PUT    /v1/users/{id}
PATCH  /v1/users/{id}/status
DELETE /v1/users/{id}
GET    /v1/users/count
GET    /v1/users/search?name=
GET    /v1/users/search/page/{page}
GET    /v1/roles
POST   /v1/roles/{id}/add-permissions
PUT    /v1/roles/{id}/permissions
DELETE /v1/roles/{id}/remove-permissions
GET    /v1/permissions
GET    /actuator/health|info
```

## Seguridad

- Resource Server validando JWT emitidos por `sgivu-auth` (issuer vía Config Server).
- Claim `rolesAndPermissions` se transforma en autoridades mediante `JwtAuthenticationConverter` y `@PreAuthorize`.
- Endpoints internos `/v1/users/username/**` exigen `X-Internal-Service-Key`; autoedición segura con `X-User-ID`.
- Contraseñas cifradas con `BCryptPasswordEncoder`.

## Dependencias

- `sgivu-config` (configuración externa, issuer JWT, tracing)
- `sgivu-discovery` (registro/balanceo)
- `sgivu-gateway` (exposición al frontend)
- `sgivu-auth` (emisión de tokens)
- PostgreSQL (usuarios, roles, permisos, direcciones)

## Dockerización

- Imagen: `sgivu-user`
- Puerto expuesto: 8081

Ejemplo:

```bash
docker build -t sgivu-user .

  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CONFIG_IMPORT=configserver:http://sgivu-config:8888 \
  -e SERVICE_INTERNAL_SECRET_KEY=... \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sgivu_user \
  -e SPRING_DATASOURCE_USERNAME=sgivu \
  -e SPRING_DATASOURCE_PASSWORD=sgivu \
  sgivu-user
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-user:v1`.
- Orquestadores externos pueden invocarlo al construir todos los servicios.

## Despliegue

- En EC2 o ECS/Fargate con Auto Scaling apuntando al gateway.
- RDS PostgreSQL con `schema.sql` y `data.sql` aplicados desde pipeline/migraciones.
- Variables requeridas: `SPRING_CONFIG_IMPORT`, `SERVICE_INTERNAL_SECRET_KEY`, `SPRING_DATASOURCE_*`, `SERVICES_SGIVU-AUTH_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `ZIPKIN_BASE_URL`.

## Monitoreo

- Actuator (`/actuator/health`, `/actuator/info`); Micrometer Tracing + Brave exporta spans a Zipkin.

## Troubleshooting

- 401/403: verifica issuer de `sgivu-auth` y claim `rolesAndPermissions`.
- Endpoints internos: asegura `X-Internal-Service-Key` igual a `SERVICE_INTERNAL_SECRET_KEY`.
- Tablas faltantes: aplica `database/schema.sql` si usas `ddl-auto: none`.
- No registra en Eureka: revisa `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y que Eureka esté activo.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
