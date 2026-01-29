# sgivu-gateway - SGIVU

## Descripción

`sgivu-gateway` es el API Gateway y BFF del ecosistema **SGIVU**. Implementado con Spring Cloud Gateway (WebFlux), actúa como:

- **BFF (Backend For Frontend)** para la SPA (provee `/auth/session`, `/auth` flows).
- **Proxy y enroutador** para todas las APIs de negocio (`/v1/*`), aplicando seguridad (token relay / JWT validation), circuit breakers, reescrituras de rutas y fallbacks.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 3.5.8
- Spring Cloud Gateway (WebFlux)
- Spring Security (OAuth2 client + resource server)
- Spring Session (Redis)
- Resilience4j (circuit breaker)
- Micrometer Tracing + Zipkin (tracing)
- SpringDoc OpenAPI (Swagger UI)
- Lombok

## Requisitos Previos

- JDK 21
- Maven 3.9+
- Docker & docker-compose
- Redis (para sesiones) — definido en `infra/compose/sgivu-docker-compose` como `sgivu-redis`
- `sgivu-config`, `sgivu-discovery` y `sgivu-auth` deben estar disponibles (o levantar la stack completa con docker-compose)

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
./build-image.bash
docker build -t stevenrq/sgivu-gateway:v1 .
docker run -p 8080:8080 --env-file infra/compose/sgivu-docker-compose/.env stevenrq/sgivu-gateway:v1
```

Puerto por defecto: `8080` (configurable via `PORT` o configserver).

## Endpoints Principales

| Endpoint | Descripción |
| --- | --- |
| `GET /auth/session` | Información de sesión BFF (subject, username, roles, isAdmin) |
| `/docs/<service>/...` | Documentación Swagger proxificada a microservicios |
| `/v1/*` | APIs de negocio (protegidas, requieren token) |
| `GET /fallback/*` | Endpoints de fallback cuando un servicio falla |
| `GET /actuator/health` | Estado de salud del servicio |

### Rutas y Filtros

- **Documentación:** `/docs/<service>/...` → reescritura y proxy a microservicios
- **APIs:** `/v1/*` → tokenRelay + circuitBreaker + fallback `forward:/fallback/<service>`
- **ML routing:** a `http://sgivu-ml:8000`

### Filtros Globales

- `ZipkinTracingGlobalFilter`: crea spans, añade `X-Trace-Id` y etiqueta spans con status/duration.
- `AddUserIdHeaderGlobalFilter`: añade header `X-User-ID` con subject/claim del token.

## Seguridad

- `sgivu-gateway` actúa como **OAuth2 client** (para login/PKCE) y como **Resource Server** (valida JWT) para rutas API.
- Configuración del cliente (registrations/providers) se encuentra en `sgivu-config-repo/sgivu-gateway.yml`.
- El gateway aplica `tokenRelay()` en rutas de backend para pasar el token del usuario a los microservicios.
- **Rutas públicas:** `/docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/oauth2/**`, `/login/**`, `/auth/session`, `/fallback/**`.
- **Rutas protegidas:** `/v1/users/**`, `/v1/persons/**`, `/v1/companies/**`, `/v1/vehicles/**`, `/v1/purchase-sales/**`, `/v1/ml/**`, etc.

> Recomendación: revisar reglas de CORS en `SecurityConfig`.

## Observabilidad

- **Tracing:** Brave/Zipkin integrado (header `X-Trace-Id` para trazabilidad). Zipkin endpoint configurado via configserver.

## Pruebas

```bash
./mvnw test
```

- Test base: `src/test/java/com/sgivu/gateway/GatewayApplicationTests.java`
- Recomendación: añadir tests de integración que validen:
  - Rutas y reescrituras de `/docs/*`
  - Propagación del token (`tokenRelay`)
  - Circuit breaker + fallback behaviors
  - Global filters (X-Trace-Id y X-User-ID)

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| 401/403 en APIs | Verificar token Bearer válido (aud/issuer) y que `sgivu-auth` esté operativo |
| Fallos en enroutamiento | Comprobar resolución de servicios en Eureka y `eureka.client.service-url.defaultZone` |
| Problemas de sesión | Comprobar Redis (`REDIS_HOST`, `REDIS_PASSWORD`) |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
