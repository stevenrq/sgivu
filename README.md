# SGIVU

## Descripción

Documentación central del sistema SGIVU (backend, frontend, ML e infraestructura) con guías de arquitectura, orquestación y scripts auxiliares.

## Tecnologías

- Backend: Spring Boot, Spring Cloud, PostgreSQL, Redis.
- Frontend: Angular.
- ML: FastAPI, scikit-learn.
- Infraestructura: Docker, Docker Compose, Nginx, AWS.
- Observabilidad: Actuator, Micrometer.

## Arquitectura

```mermaid
---
title: Arquitectura general de SGIVU
id: 057930b9-c967-4950-84d4-16f589829c2c
---
flowchart TB
    usuario((Usuario))

    subgraph edge ["Capa de Borde"]
        nginx["Nginx (Producción)"]
        gateway["API Gateway / BFF<br>(sgivu-gateway)"]
        auth["Auth Server<br>(sgivu-auth)"]
    end

    subgraph services ["Servicios de Negocio"]
        direction LR
        user["Usuarios"]
        client["Clientes"]
        vehicle["Vehículos"]
        purchase_sale["Compras/Ventas"]
    end

    ml["ML (FastAPI)"]

    subgraph data ["Datos"]
        direction LR
        postgres[("PostgreSQL")]
        redis[("Redis")]
    end

    subgraph platform ["Plataforma"]
        direction LR
        config["Config Server"]
        discovery["Eureka"]
    end

    usuario --> spa["Angular SPA"]
    spa --> nginx
    nginx -- "OIDC" --> auth
    nginx -- "API" --> gateway

    gateway --> services
    gateway -. "directo" .-> ml
    gateway --> redis
    auth -. "interno" .-> user

    services & auth & ml --> postgres

    classDef db fill:#e8f4f8,stroke:#2b7fb3,stroke-width:2px;
    class postgres,redis db;
```

> **Nota:** Cada microservicio posee su propio esquema en PostgreSQL. Redis persiste sesiones HTTP del Gateway (patrón BFF). Todos los servicios se registran en Eureka y obtienen configuración de Config Server.

## Documentación

La documentación técnica completa del proyecto está construida con Mintlify y se encuentra en el directorio `docs/`.

### Acceso Local

Para ejecutar la documentación localmente:

```bash
cd docs
npm i -g mint
mint dev
```

La documentación estará disponible en `http://localhost:3000`.

### Documentación en Producción

La documentación desplegada estará disponible en: [https://sgivu.mintlify.app](https://sgivu.mintlify.app)

## Configuración

- Configuración centralizada en `sgivu-config` y repositorio Git de configuración.
- Soporte para perfil `native` en `sgivu-config` para cargar configuraciones locales sin necesidad de Git.
- Variables de entorno base en `infra/compose/sgivu-docker-compose/.env.example`.

## Ejecución Local

- Stack completo con Docker Compose: `infra/compose/sgivu-docker-compose/run.sh --dev`.
- Operaciones puntuales del stack: `infra/compose/sgivu-docker-compose/run-service.sh` para levantar servicios aislados y `infra/compose/sgivu-docker-compose/dbs-backups.sh` para respaldos.
- Consulte `infra/compose/sgivu-docker-compose/README.md` para ejemplos y parámetros.

## Endpoints Principales

- Gateway: `http://localhost:8080`
- Auth: `http://localhost:9000`
- Config: `http://localhost:8888`
- Discovery: `http://localhost:8761`
- Frontend: `http://localhost:4200`
- ML: `http://localhost:8000`

## Seguridad

- **Patrón BFF (Backend For Frontend):** Implementado vía `sgivu-gateway`, que actúa como BFF encargado de almacenar y servir el `access_token` y el `refresh_token` necesarios para la aplicación Angular. Aunque los tokens son creados por `sgivu-auth`, el gateway centraliza su gestión.
- OAuth 2.1/OIDC con JWT emitidos por `sgivu-auth`.
- Claves internas para comunicación service-to-service.
- Nunca versionar secretos ni `.env` reales.

## Redis

Redis se usa en dos servicios, con namespaces independientes:

- **`sgivu-gateway`** — Persistencia de sesiones HTTP (patrón BFF). Tokens OAuth2 (`access_token`, `refresh_token`) y `SecurityContext` se almacenan en Redis para escalar el gateway horizontalmente sin perder sesiones. Namespace: `spring:session:sgivu-gateway`. TTL deslizante 7 días.
- **`sgivu-purchase-sale`** — Caché del dashboard (`dashboard-summary`, TTL 60 s). Definido en `CacheConfig.java`, invalidado con `@CacheEvict` en create/update/delete de contratos. Namespace: `sgivu:cache:purchase-sale:`.

Ambos comparten el mismo contenedor Redis pero no se solapan.

- **Configuración**: el config repo define la conexión en `sgivu-gateway-{dev,prod}.yml` y `sgivu-purchase-sale-{dev,prod}.yml` usando los placeholders `${DEV_REDIS_HOST}` / `${PROD_REDIS_HOST}` (y equivalentes para puerto y contraseña).
- **Cookie de sesión** (gateway): `RedisSessionConfig.java` configura `SESSION` con `HttpOnly`, `SameSite=Lax`, `Path=/`.
- **Keep-alive**: Angular hace ping a `/auth/session` cada 20 min para mantener vivo el TTL deslizante.
- **Docker**: Servicio `sgivu-redis` (imagen `redis:7`) con autenticación por contraseña y volumen `redis-data`.
- **Variables de entorno**: `DEV_REDIS_HOST` / `PROD_REDIS_HOST` (default `sgivu-redis`), `DEV_REDIS_PORT` / `PROD_REDIS_PORT` (default `6379`), `DEV_REDIS_PASSWORD` / `PROD_REDIS_PASSWORD` (requerida).
- **No se usa** para rate limiting ni operaciones directas con `RedisTemplate`.

## Servicios y Componentes

### Backend

- [sgivu-auth](apps/backend/sgivu-auth/README.md) — Servicio de autenticación y autorización (OAuth 2.1/OIDC, JWT).
- [sgivu-gateway](apps/backend/sgivu-gateway/README.md) — API Gateway (Spring Cloud Gateway, WebFlux) y BFF para la SPA: gestiona la sesión OAuth2, hace token relay y aplica circuit breakers a los servicios downstream.
- [sgivu-config](apps/backend/sgivu-config/README.md) — Servidor de configuración centralizada.
- [sgivu-discovery](apps/backend/sgivu-discovery/README.md) — Registro y descubrimiento de servicios (Eureka).
- [sgivu-user](apps/backend/sgivu-user/README.md) — Servicio de gestión de usuarios.
- [sgivu-vehicle](apps/backend/sgivu-vehicle/README.md) — Servicio de gestión de vehículos.
- [sgivu-purchase-sale](apps/backend/sgivu-purchase-sale/README.md) — Servicio de compra-venta.
- [sgivu-client](apps/backend/sgivu-client/README.md) — Servicio de gestión de clientes.

### Frontend

- [sgivu-frontend](https://github.com/stevenrq/sgivu-frontend) — Aplicación Angular (repositorio independiente, desplegado en un hosting estático en la nube).

### Machine Learning

- [sgivu-ml](apps/ml/sgivu-ml/README.md) — Servicio de ML con FastAPI.

### Infraestructura

- [sgivu-docker-compose](infra/compose/sgivu-docker-compose/README.md) — Orquestación local con Docker Compose.
- [sgivu-config-repo](https://github.com/stevenrq/sgivu-config-repo/blob/main/README.md) — Repositorio centralizado de configuración para todos los servicios (Git-based Config Server).

## Dockerización

- Cada servicio cuenta con `Dockerfile` y scripts `build-image.sh` cuando aplica.
- Stack integrado vía Docker Compose en `infra/compose/sgivu-docker-compose`.

## Build y Push Docker

- Orquestador: `infra/compose/sgivu-docker-compose/build-and-push-images.sh` (construye y publica todas las imágenes).
- Servicios individuales: `apps/**/build-image.sh` construye localmente; use `./build-image.sh --push` para publicar en Docker Hub.

## Despliegue

- Despliegue actual: **EC2 única** con Docker Compose (`infra/compose/sgivu-docker-compose/docker-compose.yml`) y Nginx en el host.
- Exponer públicamente solo Nginx (80/443) y, por Nginx, enrutar a Gateway, Auth y al bucket S3 de la SPA. El resto de servicios queda en la red interna del compose.

### Nginx (Producción)

Nginx actúa como único punto de entrada público (ver `infra/nginx/sites-available/default.conf`):

- **Auth Server** (puerto 9000): `/login`, `/oauth2/*`, `/.well-known/*` — flujos OIDC directos, sin pasar por Gateway.
- **Gateway** (puerto 8080): `/v1/*`, `/docs/*`, `/auth/session` — APIs de negocio y BFF.
- **Frontend**: S3 como fallback catch-all para la SPA Angular.

Esta separación permite escalar Auth y Gateway independientemente y simplifica reglas de firewall (solo 80/443 expuestos).

## Monitoreo

- Actuator en servicios Spring y health checks en FastAPI.

## Solución de Problemas

- Puertos ocupados: revisa mapeos en Compose y detén procesos locales.
- Config Server inaccesible: valida `SPRING_CONFIG_IMPORT` o `SPRING_CLOUD_CONFIG_URI`.

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
