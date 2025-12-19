# SGIVU - sgivu-purchase-sale

## Descripción

Microservicio que centraliza contratos de compra y venta de vehículos, validando clientes, usuarios y vehículos para registrar transacciones consistentes.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud enfocado en ciclo de contratos.
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`, `sgivu-client`, `sgivu-user`, `sgivu-vehicle`.
- Valida referencias externas vía RestClient y claves internas; persiste contratos en PostgreSQL (`purchase_sales`).
- Expuesto vía Eureka y protegido en gateway; soporta paginación y filtros por actor.

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Seguridad: OAuth 2.1 Resource Server, JWT (`rolesAndPermissions`), `@PreAuthorize`, `InternalServiceAuthorizationManager`
- Persistencia: Spring Data JPA, PostgreSQL (`schema.sql`, `data.sql`)
- Integración: Rest Client + `HttpServiceProxyFactory`, MapStruct, Jakarta Validation
- Infraestructura: Docker, Actuator, Eureka, Config Client

## Configuración

- Variables clave: `SPRING_CONFIG_IMPORT`, `SPRING_PROFILES_ACTIVE`, `SERVICE_INTERNAL_SECRET_KEY`, `services.map.*` (auth, client, user, vehicle), datasource, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- `application-local.yml` recomendado para desarrollo; `management.tracing.enabled` opcional para Zipkin.

## Ejecución Local

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Requiere Config, Discovery, Auth, Client, User, Vehicle y PostgreSQL (`schema.sql` y opcional `data.sql`). Acceso en `http://localhost:8084` o vía gateway (page size 10).

## Endpoints Principales

```text
POST   /v1/purchase-sales
GET    /v1/purchase-sales/{id}
GET    /v1/purchase-sales
GET    /v1/purchase-sales/detailed
GET    /v1/purchase-sales/page/{page}
GET    /v1/purchase-sales/page/{page}/detailed
GET    /v1/purchase-sales/search?contractType=&contractStatus=&clientId=&userId=&vehicleId=&paymentMethod=&startDate=&endDate=&minPurchasePrice=&maxPurchasePrice=&minSalePrice=&maxSalePrice=&term=&detailed=
PUT    /v1/purchase-sales/{id}
DELETE /v1/purchase-sales/{id}
GET    /v1/purchase-sales/client/{clientId}
GET    /v1/purchase-sales/user/{userId}
GET    /v1/purchase-sales/vehicle/{vehicleId}
GET    /v1/purchase-sales/report/pdf
GET    /v1/purchase-sales/report/excel
GET    /v1/purchase-sales/report/csv
GET    /actuator/health|info
```

## Seguridad

- Resource Server validando JWT de `sgivu-auth`; claim `rolesAndPermissions` mapeado a autoridades.
- Permisos: `purchase_sale:create|read|update|delete`.
- `InternalServiceAuthorizationManager` para llamadas internas con `X-Internal-Service-Key`.
- `JwtAuthorizationInterceptor` propaga el token a `sgivu-client`, `sgivu-user`, `sgivu-vehicle`.
- `/actuator/health|info` públicos; resto requiere token o clave interna.

## Dependencias

- `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`, `sgivu-client`, `sgivu-user`, `sgivu-vehicle`, PostgreSQL.

## Dockerización

- Imagen: `sgivu-purchase-sale`
- Puerto expuesto: 8084

Ejemplo:

```bash
./mvnw clean package -DskipTests
docker build -t sgivu-purchase-sale .

  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CONFIG_IMPORT=configserver:http://sgivu-config:8888 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sgivu_purchase_sale_db \
  -e SPRING_DATASOURCE_USERNAME=sgivu \
  -e SPRING_DATASOURCE_PASSWORD=sgivu \
  -e SERVICE_INTERNAL_SECRET_KEY=... \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://sgivu-discovery:8761/eureka \
  -e SERVICES_MAP_SGIVU_AUTH_URL=http://sgivu-auth:9000 \
  -e SERVICES_MAP_SGIVU_CLIENT_URL=http://sgivu-client:8082 \
  -e SERVICES_MAP_SGIVU_USER_URL=http://sgivu-user:8081 \
  -e SERVICES_MAP_SGIVU_VEHICLE_URL=http://sgivu-vehicle:8083 \
  sgivu-purchase-sale
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-purchase-sale:v1`.

## Despliegue

- Publica imagen en ECR y despliega en ECS/Fargate, EKS o EC2 detrás de gateway.
- Usa RDS PostgreSQL con `schema.sql` aplicado; gestiona secretos (`SERVICE_INTERNAL_SECRET_KEY`, `SPRING_DATASOURCE_*`, URLs internas) con Secrets Manager/Parameter Store.
- Configura Auto Scaling y health checks a `/actuator/health`; métricas a CloudWatch/Prometheus según clúster.

## Monitoreo

- Actuator (`health`, `info`, `metrics`, `prometheus` si se habilita).
- Micrometer/Zipkin vía Config Server (`management.tracing.*`, `management.zipkin.tracing.endpoint`).

## Troubleshooting

- 401/403: valida issuer de `sgivu-auth` y permisos del JWT.
- Fallas al validar referencias: revisa URLs en `services.map` y disponibilidad de servicios; usa `X-Internal-Service-Key` correcto.
- Paginación vacía: revisa filtros y datos en BD (`database/data.sql`).
- No registra en Eureka: confirma `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y estado de discovery.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
