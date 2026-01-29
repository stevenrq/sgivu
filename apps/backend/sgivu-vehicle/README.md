# sgivu-vehicle - SGIVU

## Descripción

`sgivu-vehicle` gestiona el catálogo de vehículos (autos y motocicletas) y las imágenes asociadas. Soporta CRUD, búsquedas avanzadas, gestión de estado (available/unavailable), y subida/gestión de imágenes mediante S3 con URLs prefirmadas.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 4.0.1
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway
- AWS SDK S3 (software.amazon.awssdk.s3)
- Spring Cloud Config Client
- Spring Cloud Eureka Client
- SpringDoc OpenAPI (Swagger UI)
- MapStruct, Lombok

## Requisitos Previos

- JDK 21
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
./build-image.bash
docker build -t sgivu-vehicle:local .
```

## Integraciones

- **AWS S3:** generación de URLs prefirmadas para subir y descargar imágenes (`S3Service`, `S3Presigner`).
- **CORS:** `S3BucketCorsConfig` asegura que el bucket permita los orígenes indicados en `aws.s3.allowed-origins`.
- **Comunicación interna:** `InternalServiceAuthenticationFilter` permite autenticación entre servicios mediante `X-Internal-Service-Key`.

### Flujo de Imágenes

1. Solicitar URL prefirmada al backend
2. Subir directamente a S3 con la URL
3. Confirmar upload al backend (`confirm-upload`) que valida clave y registra metadatos

> Solo se aceptan tipos `image/jpeg`, `image/png`, `image/webp`. Primera imagen registrada se marca como `is_primary=true`; al eliminar la primaria se promueve la siguiente más antigua.

## Seguridad

- **Autenticación:** JWT tokens emitidos por `sgivu-auth`. `JwtAuthenticationConverter` mapea claims a autoridades.
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
- **Tracing:** Zipkin configurado via `sgivu-config`

## Pruebas

```bash
./mvnw test
```

- `spring-boot-starter-flyway-test` puede ayudar a validar migraciones en tests.
- Recomendación: añadir pruebas de integración que simulen el flujo completo de presigned upload + confirmación.

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
