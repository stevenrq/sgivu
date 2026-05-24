# sgivu-user - SGIVU

## Descripción

`sgivu-user` gestiona usuarios, roles y permisos del ecosistema SGIVU. Proporciona CRUD de usuarios y personas, gestión de roles y permisos, búsqueda multi-criterio y endpoints de apoyo para el Authorization Server (consulta por username para emisión de JWT).

## Tecnologías y Dependencias

- Java 25
- Spring Boot 4.0.1, Spring Cloud 2025.1.0
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Cloud Config Client
- Spring Cloud Eureka Client
- SpringDoc OpenAPI 3.0.1 (Swagger UI)
- MapStruct 1.6.3, Lombok 1.18.38

## Requisitos Previos

- JDK 25
- Maven 3.9+
- PostgreSQL
- `sgivu-config` y `sgivu-discovery` disponibles (o arrancados via docker-compose)

## Arranque y Ejecución

### Desarrollo (docker-compose)

Desde `infra/compose/sgivu-docker-compose`:

```bash
docker compose -f docker-compose.dev.yml up -d
```

### Ejecución Local

```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

### Docker

```bash
./build-image.sh          # construye localmente
./build-image.sh --push   # construye y publica en Docker Hub

docker build -t sgivu-user:local .
```

## Seguridad

- **Autenticación:** JWT tokens emitidos por `sgivu-auth`. La validación usa `NimbusJwtDecoder` apuntando al issuer.
- **Claims:** `rolesAndPermissions` es usado para autorización granular en endpoints (`user:create`, `user:read`, `user:update`, `user:delete`).
- **Self-update:** los usuarios pueden modificar su propio perfil aunque no tengan `user:update` (regla `#id.toString() == #authenticatedUserId`).
- **Internal calls:** el header `X-Internal-Service-Key` permite que `sgivu-auth` consulte usuarios por username (`GET /v1/users/username/{username}`) al emitir tokens; **no exponer** esta clave.
- **Validaciones:** existen validadores personalizados para `PasswordStrength` y `NoSpecialCharacters` aplicados en creación y actualización.

## Endpoints destacados

- Usuarios: `POST/GET/PUT/DELETE /v1/users`, `/v1/users/page/{page}`, `/v1/users/{id}/status`, `/v1/users/count`, `/v1/users/batch`, `/v1/users/search`, `/v1/users/search/page/{page}`, `/v1/users/username/{username}` (interno).
- Roles: `GET /v1/roles`, `POST /v1/roles/{id}/add-permissions`, `PUT /v1/roles/{id}/permissions`, `DELETE /v1/roles/{id}/remove-permissions`.
- Permisos: `GET /v1/permissions`.

## Migraciones

- **Migración principal:** `src/main/resources/db/migration/V1__initial_schema.sql`
  - Crea tablas `permissions`, `roles`, `persons`, `users`, `roles_permissions`, `users_roles`, `addresses` y los índices necesarios.
- **Seed:** `src/main/resources/db/seed/R__seed_data.sql`
  - Crea datos de ejemplo incluyendo el usuario `steven` con rol `ADMIN` y un catálogo completo de permisos.

## Observabilidad

- **Actuator:** `/actuator/health`, `/actuator/info` (exposición configurable)
- **OpenAPI UI:** `/swagger-ui/index.html` (con servers definidos en `OpenApiConfig`)

## Pruebas

```bash
./mvnw test
```

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| Errores de autorización (401/403) | Verificar token Bearer y authorities requeridas |
| Endpoint interno falla | Comprobar `X-Internal-Service-Key` y que `sgivu-auth` tenga la clave correcta |
| Errores de BD | Verificar conexión PostgreSQL y migraciones Flyway |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
