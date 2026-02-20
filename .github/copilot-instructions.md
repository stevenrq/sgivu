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

| Servicio              | Puerto | Rol                                                  |
| --------------------- | ------ | ---------------------------------------------------- |
| `sgivu-gateway`       | 8080   | API Gateway + BFF (cliente OAuth2, relay de tokens)  |
| `sgivu-auth`          | 9000   | Servidor de Autorización (OAuth2.1/OIDC, emisor JWT) |
| `sgivu-config`        | 8888   | Spring Cloud Config Server                           |
| `sgivu-discovery`     | 8761   | Registro de Servicios Eureka                         |
| `sgivu-user`          | 8081   | Gestión de usuarios                                  |
| `sgivu-client`        | 8082   | Gestión de clientes                                  |
| `sgivu-vehicle`       | 8083   | Inventario de vehículos                              |
| `sgivu-purchase-sale` | 8084   | Transacciones de compra/venta                        |
| `sgivu-ml`            | 8000   | Predicciones ML (FastAPI)                            |

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

## Redis (Sesiones del Gateway)

Redis se usa **exclusivamente en `sgivu-gateway`** para **persistir sesiones HTTP** como parte del patrón BFF. El gateway es cliente OAuth2 y almacena los tokens (`access_token`, `refresh_token`) y el estado de sesión del usuario en la sesión web, la cual se respalda en Redis. Esto permite escalar el gateway horizontalmente sin perder sesiones.

**No se usa Redis** para rate limiting, caché (`@Cacheable`) ni operaciones directas con `RedisTemplate` en ningún servicio.

### Dependencias Maven (`sgivu-gateway/pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### Configuración (`sgivu-config-repo/sgivu-gateway.yml`)

```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: spring:session:sgivu-gateway
  data:
    redis:
      host: ${REDIS_HOST:sgivu-redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
```

### Clase de Configuración Java

`RedisSessionConfig.java` en `sgivu-gateway/config/` configura la cookie de sesión:

- Nombre: `SESSION`
- `HttpOnly=true`, `SameSite=Lax`, `Path=/`
- `SameSite=Lax` es necesario para que la cookie sobreviva los redirects OAuth2 desde `sgivu-auth`

La conexión a Redis y el repositorio de sesiones se auto-configuran por Spring Boot a partir de las dependencias y la configuración YAML.

### Docker

Servicio `sgivu-redis` en Docker Compose (`redis:7`) con autenticación por contraseña y volumen persistente `redis-data`. El gateway declara `depends_on: sgivu-redis`.

### Variables de Entorno

| Variable         | Default       | Descripción                             |
| ---------------- | ------------- | --------------------------------------- |
| `REDIS_HOST`     | `sgivu-redis` | Host del servidor Redis                 |
| `REDIS_PORT`     | `6379`        | Puerto de Redis                         |
| `REDIS_PASSWORD` | —             | Contraseña de autenticación (requerida) |

## Puntos Clave de Integración

1. **Gateway → Auth**: Registro de cliente OAuth2 en `sgivu-gateway.yml`, flujos de redirección vía `/login/oauth2/code/sgivu-gateway`
2. **Gateway → Redis**: Persistencia de sesiones HTTP (tokens OAuth2) vía `spring-session-data-redis`
3. **Auth → User**: Validación de credenciales vía `CredentialsValidationService` usando clave interna
4. **Todos los Servicios → Config**: Bootstrap desde `http://sgivu-config:8888`
5. **Todos los Servicios → Discovery**: Registro con Eureka en `http://sgivu-discovery:8761/eureka`

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

| Ruta                                                   | Servicio                   | Filtros                    |
| ------------------------------------------------------ | -------------------------- | -------------------------- |
| `/v1/users/**`, `/v1/roles/**`, `/v1/permissions/**`   | `lb://sgivu-user`          | tokenRelay, circuitBreaker |
| `/v1/persons/**`, `/v1/companies/**`                   | `lb://sgivu-client`        | tokenRelay, circuitBreaker |
| `/v1/vehicles/**`, `/v1/cars/**`, `/v1/motorcycles/**` | `lb://sgivu-vehicle`       | tokenRelay, circuitBreaker |
| `/v1/purchase-sales/**`                                | `lb://sgivu-purchase-sale` | tokenRelay, circuitBreaker |
| `/v1/ml/**`                                            | `http://sgivu-ml:8000`     | tokenRelay, circuitBreaker |

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
- **Sesiones perdidas tras reinicio del gateway**: Verificar que `sgivu-redis` esté corriendo y que `REDIS_PASSWORD` coincida entre el contenedor Redis y la configuración del gateway
- **Acceso en desarrollo local**: Agregar `sgivu-auth` a `/etc/hosts` apuntando a `127.0.0.1`

## Reglas de Generación y Modificación de Código

Sigue estrictamente estas reglas al generar o modificar código:

### Idioma

- **Código fuente** (clases, métodos, variables, archivos, logs, excepciones): **INGLÉS**.
- **Comentarios y documentación**: **ESPAÑOL**.
- **Textos visibles para el usuario** (UI, mensajes, validaciones, respuestas de error): **ESPAÑOL**.

### Pruebas

- `@DisplayName` (Spring / JUnit): **ESPAÑOL**.
- `describe()` e `it()` (Angular / Jasmine): **ESPAÑOL**.
- Nombres de métodos de test: **INGLÉS**.

### Regla Base

> Lo que lee un humano → español.
> Lo que ejecuta la máquina → inglés.

### Calidad de Código

- Aplicar **SOLID**, **Clean Code** y **DRY**.
- Clases con una sola responsabilidad.
- Métodos pequeños y legibles.
- No usar valores mágicos ni lógica hardcodeada.

### Errores y Logs

- Mensajes de error al usuario: **español**.
- Logs y errores técnicos internos: **inglés**.
- No exponer detalles técnicos al usuario.

### Nomenclatura de Pruebas

Patrón común en todos los stacks: **resultado esperado + condición**. Agrupar tests por método usando clases anidadas o bloques `describe`.

**Spring Boot (JUnit 5)** — Archivos: `<Clase>Test.java`. Usar `@Nested` + `@DisplayName` para agrupar por método, y `should<Resultado>When<Condición>` en cada test:

```java
public class UserServiceImplTest {

  @Nested
  @DisplayName("save(User)")
  class SaveUserTests {
    @Test
    @DisplayName("Debe codificar contraseña, asignar roles y guardar usuario")
    void shouldEncodePasswordAndSaveUser() { }

    @Test
    @DisplayName("Debe lanzar excepción si el rol no existe")
    void shouldThrowRoleRetrievalExceptionIfRoleNotFound() { }
  }

  @Nested
  @DisplayName("update(Long, UserUpdateRequest)")
  class UpdateUserTests {
    @Test
    @DisplayName("Debe actualizar campos y codificar contraseña si se proporciona")
    void shouldUpdateUserAndEncodePasswordIfProvided() { }
  }
}
```

**Angular (Jasmine)** — Archivos: `<archivo>.spec.ts`. Usar `describe` anidados por método, `it` como oraciones:

```ts
describe('UserService', () => {
  describe('create()', () => {
    it('Debe crear usuario y agregarlo al estado', () => { });
    it('Debe propagar error y no modificar el estado', () => { });
  });

  describe('update()', () => {
    it('Debe actualizar usuario en el estado', () => { });
    it('Debe no alterar estado si el usuario no existe', () => { });
  });
});
```

**Python (pytest)** — Archivos: `test_<modulo>.py`. Usar clases para agrupar por método, funciones con `test_<resultado>_when_<condición>`:

```python
class TestDemandService:

    class TestPredict:
        def test_returns_prediction_when_model_is_loaded(self): pass
        def test_raises_exception_when_input_is_empty(self): pass

    class TestTrain:
        def test_trains_model_when_data_is_valid(self): pass
```

**Reglas generales**:

- Describir **comportamiento**, no implementación (`shouldMapDtoToEntityCorrectly` ✅, `testMapper` ❌).
- Un test = una expectativa clara. Si el nombre tiene "and", probablemente son dos tests.
- Ser consistente en todo el proyecto: elegir un estilo y no mezclarlo.
- El nombre debe explicar el *por qué* del fallo: qué se rompió y en qué escenario.

### Diagramas UML

- Representar diagramas UML usando **PlantUML** (`.puml`) para una visualización clara y versionable.
- Los diagramas se ubican en `docs/diagrams/` y las imágenes generadas en `docs/diagrams/img/`.
- Usar comentarios en español dentro de los diagramas para describir flujos y componentes.

### Git

- Commits en **inglés**, siguiendo **Conventional Commits** y **Gitflow**.
- Commits pequeños y atómicos.
- Ramas con nombres claros (`main`, `develop`, `feature/`, `release/`, `hotfix/`, `fix/`, `refactor/`, `chore/`).
