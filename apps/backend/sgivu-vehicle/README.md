# sgivu-vehicle - SGIVU

## Descripción

`sgivu-vehicle` gestiona el catálogo de vehículos (autos y motocicletas) y las imágenes asociadas. Soporta CRUD, búsquedas avanzadas, gestión de estado vía enum `VehicleStatus` (`AVAILABLE`, `SOLD`, `IN_MAINTENANCE`, `IN_REPAIR`, `IN_USE`, `INACTIVE`) y subida/gestión de imágenes mediante S3 con URLs prefirmadas.

## Tecnologías y Dependencias

- Java 25
- Spring Boot 4.0.1, Spring Cloud 2025.1.0
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway
- AWS SDK v2 S3 (`software.amazon.awssdk.s3`)
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

docker build -t sgivu-vehicle:local .
```

## Integraciones

- **AWS S3:** generación de URLs prefirmadas para subir y descargar imágenes (`S3Service`, `S3Presigner`).
- **CORS:** `S3BucketCorsConfig` asegura que el bucket permita los orígenes indicados en `aws.s3.allowed-origins`.
- **Comunicación interna:** `InternalServiceAuthenticationFilter` permite autenticación entre servicios mediante `X-Internal-Service-Key`.

### Flujo de Imágenes (`/v1/vehicles/{vehicleId}/images`)

1. `POST /v1/vehicles/{vehicleId}/images/presigned-upload` → backend genera URL prefirmada PUT
2. El cliente sube directamente el archivo a S3 con la URL prefirmada
3. `POST /v1/vehicles/{vehicleId}/images/confirm-upload` → backend valida clave y registra metadatos
4. `GET /v1/vehicles/{vehicleId}/images` → lista imágenes del vehículo
5. `DELETE /v1/vehicles/{vehicleId}/images/{imageId}` → elimina imagen

> Solo se aceptan tipos `image/jpeg`, `image/png`, `image/webp`. La primera imagen registrada se marca como `is_primary=true`; al eliminar la primaria se promueve la siguiente más antigua.

## Seguridad

- **Autenticación:** JWT emitidos por `sgivu-auth`. `JwtAuthenticationConverter` mapea el claim `rolesAndPermissions` a autoridades.
- **Permisos por endpoint** (`@PreAuthorize`): `car:create/read/update/delete`, `motorcycle:create/read/update/delete`, `vehicle:create/read/delete` (para imágenes).
- **Internal calls:** `X-Internal-Service-Key` permite solicitudes entre servicios; **no exponer** esta clave.
- **Recomendaciones:**
  - Mover claves AWS y otros secretos a un secret manager
  - Aplicar políticas de acceso mínimo al bucket S3
  - Limitar el tiempo de las URLs prefirmadas

## Migraciones

- Migraciones Flyway en `src/main/resources/db/migration`.

## Observabilidad

- **Actuator:** `/actuator/health`, `/actuator/info`
- **OpenAPI / Swagger UI:** `/swagger-ui/index.html` (config en `OpenApiConfig`)

## Pruebas

```bash
./mvnw test
```

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| Errores de autorización (401/403) | Verificar token Bearer y authorities |
| Fallas en S3 | Comprobar credenciales AWS y permisos del bucket |
| Imágenes no se suben | Verificar tipo de contenido permitido y URL prefirmada |
| CORS errors | Revisar `aws.s3.allowed-origins` y configuración del bucket |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
