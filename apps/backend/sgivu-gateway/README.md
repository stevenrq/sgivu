# SGIVU - sgivu-gateway

## Descripción

Gateway reactivo que centraliza y enruta el tráfico HTTP del ecosistema SGIVU aplicando seguridad, resiliencia y trazabilidad.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud Gateway (WebFlux).
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-auth`, `sgivu-user`, `sgivu-client` (y otros servicios proxied).
- Registra instancias en Eureka y balancea solicitudes con `lb://`; configuración remota vía Config Server.
- Actúa como BFF: inicia sesión OIDC en `sgivu-auth`, mantiene sesión HTTP y propaga tokens hacia los microservicios.

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Gateway: Spring Cloud Gateway + Resilience4j
- Seguridad: OAuth 2.1 Resource Server + OAuth2 Client (BFF), JWT (Nimbus Reactive Decoder)
- Observabilidad: Micrometer Tracing, Brave, Zipkin, Actuator

## Configuración

- Variables clave: `SPRING_CLOUD_CONFIG_URI` o `SPRING_CONFIG_IMPORT`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `services.sgivu-auth.url`, CORS (`angular-client.url`) y OAuth2 client (`spring.security.oauth2.client.registration.sgivu-gateway.*`, `spring.security.oauth2.client.provider.sgivu-auth.*`).
- Perfiles gestionados en Config Server; ajusta rutas y filtros allí.
- `SGIVU_GATEWAY_URL` (configurado en `sgivu-auth`) solo define el `redirect_uri` usado por el navegador en el flujo OAuth2. No afecta la comunicacion interna entre microservicios.

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
GET       /auth/session       -> Estado de sesión BFF para la SPA
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

- Soporta login OIDC como BFF y valida JWT emitidos por `sgivu-auth` (`services.sgivu-auth.url`).
- Propaga access tokens a microservicios con token relay y renueva tokens en backend vía refresh tokens.
- Rutas públicas limitadas (`/v1/auth/**`, `/authorized`, `/auth`, `/user`, `/logout`, `/oauth2/**`, `/login/**`).
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
- Acceso desde host: si el navegador no resuelve `sgivu-gateway`, configura `/etc/hosts`.
  Ver `sgivu-gateway-access.md`.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
