# SGIVU - sgivu-vehicle

## Descripción

Microservicio que centraliza el inventario de vehículos (autos y motocicletas) con operaciones CRUD, búsqueda y métricas
rápidas.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud orientado a catálogo vehicular.
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`.
- Controladores REST `/v1/cars` y `/v1/motorcycles`; registro en Eureka; config desde Config Server.
- Persistencia JOINED en PostgreSQL (vehicles/cars/motorcycles) con seeds opcionales (`schema.sql`).

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Seguridad: Spring Security, OAuth 2.1 Resource Server, JWT (claim `rolesAndPermissions`)
- Persistencia: Spring Data JPA, PostgreSQL
- Infraestructura: Actuator, Lombok, Validation, Docker

## Configuración

- Variables clave: `SPRING_CONFIG_IMPORT`, `SPRING_PROFILES_ACTIVE`, `services.map.sgivu-auth.url`, datasource,
  `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- `application-local.yml` recomendado para desarrollo; controla `spring.sql.init.mode` si deseas aplicar seeds.

## Ejecución Local

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Requiere Config Server, Discovery, Auth y PostgreSQL (`database/schema.sql`). Acceso en `http://localhost:8083` o vía
gateway.

## Endpoints Principales

```text
POST   /v1/cars
GET    /v1/cars/{id}
GET    /v1/cars
GET    /v1/cars/page/{page}
PUT    /v1/cars/{id}
DELETE /v1/cars/{id}
PATCH  /v1/cars/{id}/status
GET    /v1/cars/count
GET    /v1/cars/search?... (plate, brand, line, model, fuelType, bodyType)

POST   /v1/motorcycles
GET    /v1/motorcycles/{id}
GET    /v1/motorcycles
GET    /v1/motorcycles/page/{page}
PUT    /v1/motorcycles/{id}
DELETE /v1/motorcycles/{id}
PATCH  /v1/motorcycles/{id}/status
GET    /v1/motorcycles/count
GET    /v1/motorcycles/search?... (plate, brand, line, model, motorcycleType)

POST   /v1/vehicles/{vehicleId}/images/presigned-upload
POST   /v1/vehicles/{vehicleId}/images/confirm-upload
GET    /v1/vehicles/{vehicleId}/images
DELETE /v1/vehicles/{vehicleId}/images/{imageId}

GET    /actuator/health|info
```

## Seguridad

- Resource Server validando JWT de `sgivu-auth` (`services.map.sgivu-auth.url`).
- Permisos: `car:create|read|update|delete`, `motorcycle:create|read|update|delete`.
- `GlobalExceptionHandler` entrega errores uniformes; `/actuator/health|info` son públicos.

## Dependencias

- `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`, PostgreSQL.

## Dockerización

- Imagen: `sgivu-vehicle`
- Puerto expuesto: 8083

Ejemplo:

```bash
./mvnw clean package -DskipTests
docker build -t sgivu-vehicle .

  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CONFIG_IMPORT=configserver:http://sgivu-config:8888 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sgivu_vehicle_db \
  -e SPRING_DATASOURCE_USERNAME=sgivu \
  -e SPRING_DATASOURCE_PASSWORD=sgivu \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://sgivu-discovery:8761/eureka \
  -e SERVICES_MAP_SGIVU_AUTH_URL=http://sgivu-auth:9000 \
  sgivu-vehicle
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-vehicle:v1`.

## Despliegue

- Publica imagen en ECR; despliega en ECS/EKS/EC2 con conectividad privada a Config, Discovery y Auth.
- Inyecta `SPRING_CONFIG_IMPORT`, `SPRING_DATASOURCE_*`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`,
  `SERVICES_MAP_SGIVU_AUTH_URL` vía Secrets Manager/Parameter Store o Config Server.
- Exponer solo vía gateway detrás de ALB/NLB.

## Monitoreo

- Actuator (`health`, `info`, `metrics`, `prometheus` si se habilita).
- Micrometer/Zipkin habilitables via Config Server (`management.tracing.*`).

## Troubleshooting

- 401/403: revisa URL de Auth y permisos (`car:*`, `motorcycle:*`).
- Errores 409: valida integridad y relaciones JOINED.
- No registra en Eureka: confirma `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y discovery activo.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
