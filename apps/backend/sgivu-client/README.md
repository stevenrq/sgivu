# sgivu-client - SGIVU

## Descripción

`sgivu-client` es el microservicio que gestiona información de clientes (personas y empresas) dentro del ecosistema SGIVU. Proporciona operaciones CRUD, búsqueda avanzada y contadores, exponiendo una API REST protegida por roles/permissions emitidas por el Authorization Server (`sgivu-auth`).

## Tecnologías y Dependencias

- Java 25
- Spring Boot 4.0.1, Spring Cloud 2025.1.0
- Spring Security (Resource Server)
- Spring Data JPA + PostgreSQL
- Flyway (migrations en `src/main/resources/db/migration`)
- Spring Boot Actuator
- SpringDoc OpenAPI 3.0.1
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
./mvnw clean package
./mvnw spring-boot:run
```

### Docker

```bash
docker build -t stevenrq/sgivu-client:0.1.0 .
```

### Puertos

- `sgivu-client` por defecto escucha en el puerto `8082` (configurable via `PORT` o configserver).

## Seguridad

- `sgivu-client` actúa como **Resource Server**. Valida tokens JWT emitidos por `sgivu-auth`.
- Requiere authorities/permissions para cada endpoint:
  - Personas: `person:create`, `person:read`, `person:update`, `person:delete`
  - Empresas: `company:create`, `company:read`, `company:update`, `company:delete`
- Ver `@PreAuthorize` en controladores `PersonController` y `CompanyController`.
- Asegurarse de que el Gateway o los clientes envíen el token Bearer y que `sgivu-auth` sea la autoridad emisora.

## Endpoints destacados

- Personas (`/v1/persons`): `POST/GET/PUT/DELETE`, `/page/{page}`, `/{id}/status`, `/count`, `/batch`, `/search`, `/search/page/{page}`.
- Empresas (`/v1/companies`): mismos endpoints CRUD + búsqueda + paginación.

## Migraciones

- Migraciones Flyway en `src/main/resources/db/migration/V1__initial_schema.sql` (crea tablas `addresses`, `clients`, `persons`, `companies` y sus índices). `Client` es la entidad abstracta padre de `Person` (con `nationalId` único) y `Company` (con `taxId` y `companyName` únicos).
- Configuración de datasource y flyway en `sgivu-config-repo/sgivu-client-*.yml` (dev/prod).

## Observabilidad

- **Actuator:** health/info.

## Pruebas

```bash
./mvnw test
```

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| `401`/`403` | Verificar token Bearer y que contenga las authorities requeridas |
| Errores DB | Comprobar `DEV_CLIENT_DB_*`/`PROD_CLIENT_DB_*` y que Flyway haya aplicado migraciones |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
