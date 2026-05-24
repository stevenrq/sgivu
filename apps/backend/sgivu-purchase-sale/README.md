# sgivu-purchase-sale - SGIVU

## Descripción

`sgivu-purchase-sale` gestiona contratos de compra/venta de vehículos: creación, búsqueda avanzada, reportes (PDF/XLSX/CSV) y gestión del ciclo de vida de los contratos. Está diseñado para integrarse con `sgivu-user`, `sgivu-client` y `sgivu-vehicle` para enriquecer datos y con `sgivu-auth` para autorización.

## Tecnologías y Dependencias

- Java 25
- Spring Boot 4.0.1, Spring Cloud 2025.1.0
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway (migraciones + seed `R__demo_data.sql` con ~12 000 contratos sintéticos)
- Spring Cloud Config Client & Eureka Client
- SpringDoc OpenAPI 3.0.1 (Swagger UI)
- MapStruct 1.6.3, Lombok 1.18.38
- OpenPDF 1.3.40, Apache POI 5.2.5 (reportes PDF/XLSX/CSV)
- Spring Cache + Caffeine (L1) y Spring Data Redis (L2) para `dashboard-summary` (TTL ~60 s)

## Requisitos Previos

- JDK 25
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
./build-image.sh          # construye localmente
./build-image.sh --push   # construye y publica en Docker Hub

docker build -t sgivu-purchase-sale:local .
```

## Seguridad

- **Autenticación:** JWT emitidos por `sgivu-auth`. `JwtAuthenticationConverter` mapea el claim `rolesAndPermissions` a autoridades.
- **Permisos por endpoint** (`@PreAuthorize`): `purchase_sale:create/read/update/delete`.
- **Internal calls:** `X-Internal-Service-Key` permite solicitudes entre servicios; **no exponer** esta clave.

## Endpoints destacados

- CRUD: `POST/GET/PUT/DELETE /v1/purchase-sales`
- Listados paginados/enriquecidos: `/page/{page}`, `/detailed`, `/page/{page}/detailed`
- Búsqueda: `/search` (filtra por tipo, estado, método de pago, IDs y rangos)
- Lookups: `/client/{id}`, `/user/{id}`, `/vehicle/{id}`, `/available-vehicles`
- Dashboard agregado (cacheado L1+L2): `/dashboard-summary`
- Reportes: `/report/pdf`, `/report/excel`, `/report/csv` (filtros opcionales `startDate`, `endDate`)
- Historial de estados: `GET /v1/contract-status-history/contract/{purchaseSaleId}`

## Migraciones

- `V1__initial_schema.sql`: crea la tabla `purchase_sales` con índices en `client_id`, `user_id`, `vehicle_id`, `contract_status`, `contract_type`, `created_at`.
- `V2__add_contract_status_history.sql`: añade la tabla `contract_status_history` para auditar transiciones de estado.
- `R__demo_data.sql` (repetible, solo dev): genera ~12 000 contratos sintéticos a lo largo de 36 meses con un modelo de demanda estacional para alimentar el servicio ML.

### Enums

- `ContractType`: `PURCHASE`, `SALE`.
- `ContractStatus`: `PENDING`, `ACTIVE`, `COMPLETED`, `CANCELED`.
- `PaymentMethod`: `CASH`, `BANK_TRANSFER`, `BANK_DEPOSIT`, `CASHIERS_CHECK`, `MIXED`, `FINANCING`, `DIGITAL_WALLET`, `TRADE_IN`, `INSTALLMENT_PAYMENT`.

## Observabilidad

- **Actuator:** `/actuator/health`, `/actuator/metrics` (configurable via `sgivu-config`)
- **OpenAPI:** `/swagger-ui/index.html` (cuando `springdoc` esté habilitado)

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
