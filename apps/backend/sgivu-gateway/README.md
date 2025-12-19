# SGIVU - sgivu-gateway

## Descripción

Gateway reactivo que centraliza y enruta el tráfico HTTP del ecosistema SGIVU aplicando seguridad, resiliencia y trazabilidad.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud Gateway (WebFlux).
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-auth`, `sgivu-user`, `sgivu-client` (y otros servicios proxied).
- Registra instancias en Eureka y balancea solicitudes con `lb://`; configuración remota vía Config Server.
- Stateless; actúa como proxy con filtros de seguridad y observabilidad.

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Gateway: Spring Cloud Gateway + Resilience4j
- Seguridad: OAuth 2.1 Resource Server, JWT (Nimbus Reactive Decoder)
- Observabilidad: Micrometer Tracing, Brave, Zipkin, Actuator

## Configuración

- Variables clave: `SPRING_CLOUD_CONFIG_URI` o `SPRING_CONFIG_IMPORT`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `services.sgivu-auth.url`, CORS (`angular-client.url`).
- Perfiles gestionados en Config Server; ajusta rutas y filtros allí.

## Ejecución Local

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_CLOUD_CONFIG_URI=http://localhost:8888
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka
./mvnw spring-boot:run
```

Accede a `http://localhost:8080` para consumir rutas proxied.

## Endpoints Principales

```text
GET/POST /v1/auth/**          -> Proxy sgivu-auth
GET       /v1/users/**        -> Proxy sgivu-user
GET       /v1/roles/**        -> Proxy sgivu-user
GET       /v1/permissions/**  -> Proxy sgivu-user
GET       /v1/persons/**      -> Proxy sgivu-client
GET       /v1/companies/**    -> Proxy sgivu-client
GET       /v1/vehicles/**     -> Proxy sgivu-vehicle
GET       /v1/cars/**         -> Proxy sgivu-vehicle
GET       /v1/motorcycles/**  -> Proxy sgivu-vehicle
GET       /v1/purchase-sales/** -> Proxy sgivu-purchase-sale
GET       /v1/ml/**           -> Proxy sgivu-ml
GET       /fallback/*         -> Fallback 503 controlado
```

## Seguridad

- Valida JWT emitidos por `sgivu-auth` (`services.sgivu-auth.url`).
- Rutas públicas limitadas (`/v1/auth/**`, `/authorized`, `/auth`, `/user`, `/logout`).
- Rutas internas requieren autenticación; convierte `rolesAndPermissions` a autoridades y propaga `X-User-ID`.
- CORS dinámico basado en `angular-client.url` desde Config Server.

## Dependencias

- `sgivu-config`, `sgivu-discovery`, `sgivu-auth`, `sgivu-user`, `sgivu-client`, `sgivu-vehicle`, `sgivu-purchase-sale`, `sgivu-ml`; Zipkin/Prometheus opcionales.

## Dockerización

- Imagen: `sgivu-gateway`
- Puerto expuesto: 8080

Ejemplo:

```bash
./mvnw clean package -DskipTests
docker build -t sgivu-gateway .

  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CLOUD_CONFIG_URI=http://sgivu-config:8888 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://sgivu-discovery:8761/eureka \
  sgivu-gateway
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-gateway:v1`.

## Despliegue

- Publica imagen en ECR y despliega en EC2/ECS con ALB; acceso a VPC privada con Config, Discovery, Zipkin y bases.
- Variables: `SPRING_PROFILES_ACTIVE=prod`, `SPRING_CLOUD_CONFIG_URI`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `ZIPKIN_BASE_URL` si aplica.

## Monitoreo

- `ZipkinTracingGlobalFilter` genera spans y `X-Trace-Id`; Micrometer Tracing exportable a Prometheus/CloudWatch.
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/prometheus` (si se expone en config).

## Troubleshooting

- CORS bloqueado: ajusta `angular-client.url` en Config Server.
- 401/invalid_token: valida issuer y JWKS de `sgivu-auth`.
- 503/fallback: revisar estado de servicios proxied y circuit breakers.
- No aparece en Eureka: confirma `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y conectividad.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
