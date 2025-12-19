# SGIVU - sgivu-client

## Descripción

Servicio para administrar la información de clientes (personas y compañías), centralizando datos de contacto y trazabilidad para el resto de microservicios.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud.
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`.
- APIs REST para personas y empresas; registro en Eureka; configuración desde Config Server.
- Persistencia en PostgreSQL con herencia (clients, persons, companies, addresses).

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Seguridad: OAuth 2.1 Resource Server + JWT (claim `rolesAndPermissions`)
- Persistencia: Spring Data JPA, PostgreSQL
- Infraestructura: Docker, AWS (EC2, RDS, S3)

## Configuración

- Variables clave: `SPRING_CONFIG_IMPORT`, `SPRING_PROFILES_ACTIVE`, `services.map.sgivu-auth.url`, propiedades de datasource, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
- `application-local.yml` sugerido para desarrollo con placeholders.

## Ejecución Local

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Requiere Config Server, Discovery, Gateway, Auth y PostgreSQL con `schema.sql` aplicado.

## Endpoints Principales

```text
GET    /v1/persons
GET    /v1/persons/{id}
GET    /v1/persons/page/{page}
GET    /v1/persons/count
GET    /v1/persons/search?name=&email=&nationalId=&phoneNumber=&enabled=&city=
GET    /v1/persons/search/page/{page}?name=&email=&nationalId=&phoneNumber=&enabled=&city=
POST   /v1/persons
PUT    /v1/persons/{id}
PATCH  /v1/persons/{id}/status
DELETE /v1/persons/{id}

GET    /v1/companies
GET    /v1/companies/{id}
GET    /v1/companies/page/{page}
GET    /v1/companies/count
GET    /v1/companies/search?taxId=&companyName=&email=&phoneNumber=&enabled=&city=
GET    /v1/companies/search/page/{page}?taxId=&companyName=&email=&phoneNumber=&enabled=&city=
POST   /v1/companies
PUT    /v1/companies/{id}
PATCH  /v1/companies/{id}/status
DELETE /v1/companies/{id}
```

## Seguridad

- Resource Server validando JWT de `sgivu-auth` (`services.map.sgivu-auth.url`).
- Permisos: `person:create|read|update|delete`, `company:create|read|update|delete`.
- `/actuator/health` y `/actuator/info` públicos para chequeos.

## Dependencias

- `sgivu-config`, `sgivu-discovery`, `sgivu-gateway`, `sgivu-auth`, PostgreSQL.

## Dockerización

- Imagen: `sgivu-client`
- Puerto expuesto: 8082

Ejemplo:

```bash
./mvnw clean package -DskipTests
docker build -t sgivu-client .

```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-client:v1`.

## Despliegue

- Publica imagen en ECR y despliega en EC2/ECS/EKS con acceso a Config, Discovery y Auth.
- Inyecta `SPRING_CONFIG_IMPORT`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `SPRING_DATASOURCE_*`, `services.map.sgivu-auth.url` vía Config Server o entorno.
- Balanceo vía gateway con Auto Scaling.

## Monitoreo

- Actuator (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).
- Micrometer + Zipkin configurables vía Config Server.

## Troubleshooting

- 401/403: revisa issuer y permisos (`person:*`, `company:*`).
- Búsquedas vacías: valida datos seed o registros en BD.
- No registra en Eureka: confirma `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y disponibilidad de discovery.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
