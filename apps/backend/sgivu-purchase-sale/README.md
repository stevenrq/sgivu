# sgivu-purchase-sale - SGIVU

## Descripción

`sgivu-purchase-sale` gestiona contratos de compra/venta de vehículos: creación, búsqueda avanzada, reportes (PDF/XLSX/CSV) y gestión del ciclo de vida de los contratos. Está diseñado para integrarse con `sgivu-user`, `sgivu-client` y `sgivu-vehicle` para enriquecer datos y con `sgivu-auth` para autorización.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 4.0.1
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Cloud Config Client & Eureka Client
- SpringDoc OpenAPI (Swagger UI)
- MapStruct, Lombok
- OpenPDF, Apache POI (reportes)

## Requisitos Previos

- JDK 21
- Maven 3.9+
- PostgreSQL
- `sgivu-config` y `sgivu-discovery` disponibles (o levantar la stack completa con docker-compose)

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
docker build -t sgivu-purchase-sale:local .
```

## Seguridad

- **Autenticación:** JWT tokens emitidos por `sgivu-auth`. `JwtAuthenticationConverter` mapea claim `rolesAndPermissions` a autoridades.
- **Internal calls:** `X-Internal-Service-Key` permite solicitudes entre servicios; **no exponer** esta clave.

## Migraciones

- Migración principal: `src/main/resources/db/migration/V1__initial_schema.sql`
- Crea tabla `purchase_sales` con índices en `client_id`, `user_id`, `vehicle_id`, `contract_status`, `contract_type`, `created_at`.

## Observabilidad

- **Actuator:** `/actuator/health`, `/actuator/metrics` (configurable via `sgivu-config`)
- **OpenAPI:** `/swagger-ui/index.html` (cuando `springdoc` esté habilitado)
- **Tracing:** Brave/Zipkin configurables desde `sgivu-config`.

## Pruebas

```bash
./mvnw test
```

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| Errores de autorización (401/403) | Comprobar token Bearer y que `sgivu-auth` esté operativo |
| Fallas en integraciones | Verificar que `sgivu-config` y `sgivu-discovery` estén accesibles |
| Problemas con reportes | Revisar dependencias de OpenPDF/Apache POI y límites de memoria |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
