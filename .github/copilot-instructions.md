# SGIVU - Instrucciones para Agentes de IA

## Descripción del Proyecto

SGIVU es una plataforma de microservicios para gestión de vehículos construida con **Spring Boot 4.0.1**, **Spring Cloud 2025.1.0**, **Angular 21** y **FastAPI**. La arquitectura sigue el patrón **BFF (Backend For Frontend)** donde `sgivu-gateway` maneja los flujos de autenticación y gestión de tokens para la SPA Angular.

## Arquitectura

```text
Angular SPA → Gateway (BFF) → Microservicios → PostgreSQL/MySQL
                  ↓
            Config Server ← sgivu-config-repo (archivos YAML)
                  ↓
            Eureka Discovery
```

### Límites de Servicios

| Servicio | Puerto | Rol |
| -------- | ------ | --- |
| `sgivu-gateway` | 8080 | API Gateway + BFF (cliente OAuth2, relay de tokens) |
| `sgivu-auth` | 9000 | Servidor de Autorización (OAuth2.1/OIDC, emisor JWT) |
| `sgivu-config` | 8888 | Spring Cloud Config Server |
| `sgivu-discovery` | 8761 | Registro de Servicios Eureka |
| `sgivu-user` | 8081 | Gestión de usuarios |
| `sgivu-client` | 8082 | Gestión de clientes |
| `sgivu-vehicle` | 8083 | Inventario de vehículos |
| `sgivu-purchase-sale` | 8084 | Transacciones de compra/venta |
| `sgivu-ml` | 8000 | Predicciones ML (FastAPI) |

## Comandos Esenciales

### Ejecutar Stack Completo (Desarrollo)

```bash
cd infra/compose/sgivu-docker-compose
./run.bash --dev
```

### Construir y Publicar Imagen de Servicio

```bash
cd apps/backend/sgivu-<servicio>
./build-image.bash  # Construye JAR, imagen Docker y publica en registro
```

### Reconstruir un Servicio en Stack Activo

```bash
cd infra/compose/sgivu-docker-compose
./rebuild-service.bash --dev sgivu-auth
```

### Ejecutar Tests

```bash
./mvnw test
```

## Gestión de Configuración

La configuración reside en el repositorio **separado `sgivu-config-repo`**:

- `{servicio}.yml` — Configuración base/común
- `{servicio}-dev.yml` — Sobrescrituras para desarrollo
- `{servicio}-prod.yml` — Sobrescrituras para producción

**Patrón**: Usar placeholders de variables de entorno `${VAR_NAME:default}` para secretos. Nunca versionar secretos reales.

Ejemplo: `sgivu-config-repo/sgivu-auth.yml` define keystore JWT, URL del issuer y mapeos de servicios.

## Convenciones de Código

### Estructura de Paquetes Java (por servicio)

```text
com.sgivu.<servicio>/
├── config/           # Beans de Spring, configuraciones RestClient
├── controller/       # Endpoints REST
├── dto/              # Objetos de request/response
├── entity/           # Entidades JPA
├── exception/        # Excepciones personalizadas
├── repository/       # Spring Data JPA
├── security/         # SecurityConfig, filtros, authorization managers
├── service/          # Lógica de negocio
└── <Servicio>Application.java
```

### Patrones de Seguridad

**Configuración Resource Server** (ver `sgivu-user/security/SecurityConfig.java`):

```java
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(convert())))
    .authorizeHttpRequests(authz -> authz
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/v1/users/username/**").access(internalServiceAuthManager)
        .requestMatchers("/v1/users/**").authenticated())
```

**Autenticación Service-to-Service**: Los endpoints internos usan el header `X-Internal-Service-Key` validado por `InternalServiceAuthorizationManager`. Esto permite que `sgivu-auth` consulte datos de usuario sin un token de usuario.

### Migraciones de Base de Datos

Migraciones Flyway en `src/main/resources/db/migration/`. Los archivos siguen el nombrado `V{n}__{descripcion}.sql`.

## Puntos Clave de Integración

1. **Gateway → Auth**: Registro de cliente OAuth2 en `sgivu-gateway.yml`, flujos de redirección vía `/login/oauth2/code/sgivu-gateway`
2. **Auth → User**: Validación de credenciales vía `CredentialsValidationService` usando clave interna
3. **Todos los Servicios → Config**: Bootstrap desde `http://sgivu-config:8888`
4. **Todos los Servicios → Discovery**: Registro con Eureka en `http://sgivu-discovery:8761/eureka`

## Frontend (Angular)

Ubicado en `apps/frontend/sgivu-frontend`. Comunica exclusivamente a través del gateway:

- `/auth/session` — Información de sesión (endpoint BFF)
- `/v1/*` — Rutas API protegidas (relay de token)

Build: `npm run build`
Servidor dev: `npm run start` (puerto 4200)

## Servicio ML (FastAPI)

Ubicado en `apps/ml/sgivu-ml`. Python 3.12 con scikit-learn.

- Valida JWT vía descubrimiento OIDC
- Acepta `X-Internal-Service-Key` para llamadas internas
- Rutas expuestas vía gateway en `/v1/ml/**`

## Rutas del Gateway

El gateway (`GatewayRoutesConfig.java`) define el enrutamiento a microservicios usando balanceo de carga vía Eureka (`lb://`):

| Ruta | Servicio | Filtros |
| ---- | -------- | ------- |
| `/v1/users/**`, `/v1/roles/**`, `/v1/permissions/**` | `lb://sgivu-user` | tokenRelay, circuitBreaker |
| `/v1/persons/**`, `/v1/companies/**` | `lb://sgivu-client` | tokenRelay, circuitBreaker |
| `/v1/vehicles/**`, `/v1/cars/**`, `/v1/motorcycles/**` | `lb://sgivu-vehicle` | tokenRelay, circuitBreaker |
| `/v1/purchase-sales/**` | `lb://sgivu-purchase-sale` | tokenRelay, circuitBreaker |
| `/v1/ml/**` | `http://sgivu-ml:8000` | tokenRelay, circuitBreaker |

**Swagger UI por servicio**: `/docs/<servicio>/swagger-ui.html` → reescrito a `/<servicio>/swagger-ui/*`

**Fallbacks**: Cada circuit breaker redirige a `/fallback/<servicio>` (ver `FallbackController.java`)

## Convenciones de Controladores

**Patrón Interface + Implementación**: Los controladores implementan interfaces en `controller/api/` que definen anotaciones OpenAPI:

```java
// controller/api/UserApi.java - Define contrato y documentación OpenAPI
@Tag(name = "Usuarios", description = "Operaciones CRUD...")
@RequestMapping("/v1/users")
public interface UserApi {
  @Operation(summary = "Alta de usuario")
  @PostMapping
  ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody User user, BindingResult bindingResult);
}

// controller/UserController.java - Implementación limpia
@RestController
public class UserController implements UserApi {
  @Override
  @PreAuthorize("hasAuthority('user:create')")
  public ResponseEntity<ApiResponse<UserResponse>> create(User user, BindingResult bindingResult) { ... }
}
```

**Autorización**: Usar `@PreAuthorize("hasAuthority('<recurso>:<accion>')")` en métodos del controlador.

## Observabilidad

- **Actuator**: `/actuator/health`, `/actuator/info` públicos; otros endpoints según perfil
- **Zipkin**: Trazas distribuidas en `http://localhost:9411` (dev)
- **Eureka Dashboard**: `http://localhost:8761` para verificar instancias registradas

## Despliegue (Producción)

Nginx actúa como punto de entrada único (ver `infra/nginx/sites-available/default.conf`):

- Auth Server (puerto 9000): `/login`, `/oauth2/*`, `/.well-known/*`
- Gateway (puerto 8080): `/v1/*`, `/docs/*`, `/auth/session`
- Frontend: S3 como fallback catch-all

## Patrón Dockerfile

Todos los servicios Java usan el mismo patrón:

```dockerfile
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
COPY ./target/sgivu-<servicio>-0.0.1-SNAPSHOT.jar sgivu-<servicio>.jar
EXPOSE <puerto>
ENTRYPOINT ["java", "-jar", "sgivu-<servicio>.jar"]
```

## Solución de Problemas

- **Configuración no carga**: Asegurar que `sgivu-config` inicie antes que otros servicios (verificar `depends_on` en compose)
- **Errores de issuer mismatch**: Verificar que `ISSUER_URL` coincida con la URL real usada por clientes
- **Falla autenticación interna entre servicios**: Verificar que `SERVICE_INTERNAL_SECRET_KEY` coincida en todos los servicios
- **Acceso en desarrollo local**: Agregar `sgivu-auth` a `/etc/hosts` apuntando a `127.0.0.1`
