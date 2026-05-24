# sgivu-gateway - SGIVU

## Descripción

`sgivu-gateway` es el API Gateway y BFF del ecosistema **SGIVU**. Implementado con Spring Cloud Gateway (WebFlux), actúa como:

- **BFF (Backend For Frontend)** para la SPA (provee `/auth/session`, `/auth` flows).
- **Proxy y enroutador** para todas las APIs de negocio (`/v1/*`), aplicando seguridad (token relay / JWT validation), circuit breakers, reescrituras de rutas y fallbacks.

## Tecnologías y Dependencias

- Java 25
- Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Spring Cloud Gateway Server (WebFlux reactivo)
- Spring Security (OAuth2 client + resource server)
- Spring Session Data Redis (sesiones BFF, namespace `spring:session:sgivu-gateway`, TTL 7d)
- Spring Boot Data Redis Reactive
- Spring Cloud Circuit Breaker (Resilience4j Reactor)
- SpringDoc OpenAPI (agregación de Swagger UI)
- Lombok

## Requisitos Previos

- JDK 25
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
./build-image.sh          # construye localmente
./build-image.sh --push   # construye y publica en Docker Hub

docker build -t stevenrq/sgivu-gateway:0.1.0 .
docker run -p 8080:8080 --env-file infra/compose/sgivu-docker-compose/.env stevenrq/sgivu-gateway:0.1.0
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

Las rutas se definen en código (`GatewayRoutesConfig.java`):

- **Documentación:** `/docs/{client|auth|gateway|user|vehicle|purchase-sale}/...` → reescritura y proxy a Swagger UI de cada microservicio
- **`/v1/auth/**`** → `lb://sgivu-auth` (solo `circuitBreaker`, **sin tokenRelay**)
- **`/v1/persons/**`, `/v1/companies/**`** → `lb://sgivu-client` (con tokenRelay + circuitBreaker)
- **`/v1/users/**`, `/v1/roles/**`, `/v1/permissions/**`** → `lb://sgivu-user`
- **`/v1/vehicles/**`, `/v1/cars/**`, `/v1/motorcycles/**`** → `lb://sgivu-vehicle`
- **`/v1/purchase-sales/**`** → `lb://sgivu-purchase-sale`
- **`/v1/ml/retrain`** → `${SGIVU_ML_URL}` con `mlRetrainCircuitBreaker` y timeout 1800 s (30 min)
- **`/v1/ml/**`** → `${SGIVU_ML_URL}` (FastAPI, sin Eureka)

### Filtros Globales

- `AddUserIdHeaderGlobalFilter`: añade el header `X-User-ID` con el subject del JWT (o claim `userId` para principals OIDC) en las peticiones downstream.

## Seguridad

- `sgivu-gateway` actúa como **OAuth2 client** (Authorization Code + PKCE para login) y como **Resource Server** (valida JWT) para rutas API.
- Configuración del cliente (registrations/providers, scopes `openid profile email phone address offline_access api read write`) se encuentra en `sgivu-config-repo/sgivu-gateway.yml`.
- Las sesiones del BFF se persisten en Redis (HttpOnly cookie `SESSION`, SameSite=Lax, TTL 7d sliding).
- El gateway aplica `tokenRelay()` en rutas de backend para pasar el token del usuario a los microservicios; el refresh automático lo gestiona `RefreshTokenReactiveOAuth2AuthorizedClientProvider`.
- **Rutas públicas:** `/docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/oauth2/**`, `/login/**`, `/auth/session`, `/fallback/**`.
- **Rutas protegidas:** `/v1/users/**`, `/v1/persons/**`, `/v1/companies/**`, `/v1/vehicles/**`, `/v1/cars/**`, `/v1/motorcycles/**`, `/v1/purchase-sales/**`, `/v1/ml/**`, `/v1/roles/**`, `/v1/permissions/**`.

> Recomendación: revisar reglas de CORS en `SecurityConfig`.

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
| Problemas de sesión | Comprobar Redis (`DEV_REDIS_HOST` / `PROD_REDIS_HOST`, `DEV_REDIS_PASSWORD` / `PROD_REDIS_PASSWORD`) |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
