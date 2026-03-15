# SGIVU - Instrucciones para Agentes de IA

## Descripción del Proyecto

SGIVU es una plataforma de microservicios para gestión de vehículos construida con **Java 25**, **Spring Boot**, **Spring Cloud**, **Angular 21** y **FastAPI**. La arquitectura sigue el patrón **BFF (Backend For Frontend)** donde `sgivu-gateway` maneja los flujos de autenticación y gestión de tokens para la SPA Angular.

### Versiones por Tipo de Servicio

| Grupo                                                                | Spring Boot | Spring Cloud |
| -------------------------------------------------------------------- | ----------- | ------------ |
| Servicios de negocio (auth, user, client, vehicle, purchase-sale)    | 4.0.1       | 2025.1.0     |
| Infraestructura (config, discovery, gateway)                         | 3.5.8       | 2025.0.0     |

**Dependencias comunes** en servicios de negocio: MapStruct 1.6.3, Lombok 1.18.38, SpringDoc OpenAPI 3.0.1, Flyway, PostgreSQL.

## Entorno de Desarrollo

El entorno de desarrollo local principal es **Windows 11** con **WSL (Windows Subsystem for Linux)**.

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

### Base de Datos Compartida

Un **solo contenedor PostgreSQL 16** aloja **8 bases de datos** separadas: `auth`, `user`, `client`, `vehicle`, `purchase_sale`, `gateway_session`, `zipkin`, `ml`. El script `infra/compose/sgivu-docker-compose/postgres-init/create-databases.sh` las crea automáticamente al iniciar el contenedor. MySQL 8 se usa exclusivamente para almacenamiento de Zipkin.

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

### Modos del Config Server

- **Desarrollo**: Perfil `native` + montaje local del directorio `sgivu-config-repo` vía Docker Compose (bind mount).
- **Producción**: Perfil `git` + clone vía HTTPS desde el repositorio GitHub.

### Bootstrap de Servicios

Los servicios **no** usan `bootstrap.yml`. Cargan la configuración vía `spring.config.import` en `application.yml`:

```yaml
spring:
  application:
    name: sgivu-user
  config:
    import: configserver:http://sgivu-config:8888
```

### Variables de Entorno y Plantillas

Las plantillas versionadas `.env.example` y `.env.dev.example` están en `infra/compose/sgivu-docker-compose/`. Los archivos `.env` reales están en `.gitignore`. Grupos principales:

| Grupo          | Variables clave                                                                                    |
| -------------- | -------------------------------------------------------------------------------------------------- |
| Bases de datos | `DEV_*_DB_HOST`, `DEV_*_DB_USER`, `PROD_*_DB_*`                                                    |
| URLs           | `SGIVU_AUTH_URL`, `SGIVU_GATEWAY_URL`, `ISSUER_URL`                                                |
| Secretos       | `SGIVU_GATEWAY_SECRET`, `SERVICE_INTERNAL_SECRET_KEY`, `JWT_KEYSTORE_PASSWORD`, `REDIS_PASSWORD`   |
| AWS            | `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_VEHICLES_BUCKET`                                          |
| ML             | `SGIVU_AUTH_DISCOVERY_URL`, `ENVIRONMENT`                                                          |

## Convenciones de Código

### Estructura de Paquetes Java (por servicio)

```text
com.sgivu.<servicio>/
├── config/           # Beans de Spring, configuraciones RestClient
├── controller/
│   └── api/          # Interfaces con anotaciones OpenAPI
├── dto/              # Objetos de request/response, SearchCriteria
├── entity/           # Entidades JPA
├── exception/        # GlobalExceptionHandler, excepciones personalizadas
├── mapper/           # Interfaces MapStruct
├── repository/       # Spring Data JPA
├── security/         # SecurityConfig, filtros, authorization managers
├── service/          # Lógica de negocio (interface + impl)
├── specification/    # JPA Specifications para filtrado dinámico
├── validation/       # ValidationGroups, validators personalizados
└── <Servicio>Application.java
```

### Entidades y Herencia

`Person` es la clase abstracta base mapeada a la tabla `persons` con estrategia `@Inheritance(strategy = InheritanceType.JOINED)`. Las entidades concretas (`User`, `Client`, etc.) extienden `Person` con `@PrimaryKeyJoinColumn`. **No** se usa `BaseEntity`, auditing (`@CreatedDate`, `@LastModifiedDate`) ni soft delete.

```java
@Entity
@Table(name = "persons")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "persons_id_seq")
  @SequenceGenerator(name = "persons_id_seq", sequenceName = "persons_id_seq", allocationSize = 1)
  private Long id;
  // ...
}

@Entity
@Table(name = "users")
@PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
public class User extends Person { /* ... */ }
```

### Mapeo con MapStruct

**Exclusivamente MapStruct** (no ModelMapper ni mapeo manual). Interfaces con `@Mapper(componentModel = "spring")` y anotaciones `@Mapping` explícitas:

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
  @Mapping(source = "id", target = "id")
  @Mapping(source = "roles", target = "roles")
  UserResponse toUserResponse(User user);
}
```

### Filtrado Dinámico con Specifications

Clase utilitaria `final` con constructor privado. Métodos estáticos retornan `Specification<T>`, encadenados con `.and()`:

```java
public final class UserSpecifications {
  private UserSpecifications() {}

  public static Specification<User> withFilters(UserFilterCriteria criteria) {
    Specification<User> spec = Specification.unrestricted();
    if (StringUtils.hasText(criteria.getName())) {
      spec = spec.and(nameContains(criteria.getName().trim()));
    }
    return spec;
  }
}
```

Los repositorios extienden `JpaSpecificationExecutor<T>`:

```java
public interface UserRepository extends PersonRepository<User>, JpaSpecificationExecutor<User> { }
```

### Validación Condicional

`ValidationGroups` define interfaces marcadoras para aplicar reglas distintas entre creación y actualización:

```java
public interface ValidationGroups {
  interface Create {}
  interface Update {}
}
```

Uso en entidades con `groups`:

```java
@NotBlank(groups = Create.class)
@PasswordStrength(groups = Create.class)
private String password;
```

Validators personalizados: `@PasswordStrength` (entropía mínima, caracteres especiales), `@NoSpecialCharacters` (username, national ID).

### Paginación

Patrón consistente: `Page<Entity>.map(mapper::toResponse)` para transformar respuestas paginadas:

```java
Page<CarResponse> page = carService.findAll(PageRequest.of(page, size))
    .map(vehicleMapper::toCarResponse);
```

### Comunicación Service-to-Service (RestClient)

Los servicios usan el patrón moderno `RestClient` + `@LoadBalanced` + `HttpServiceProxyFactory` para crear proxies tipados a partir de interfaces `@HttpExchange`:

```java
// Interfaz del cliente (en el servicio que consume)
@HttpExchange("/v1/users")
public interface UserClient {
  @GetExchange("/{id}")
  User getUserById(@PathVariable Long id);
}
```

```java
// Configuración del bean (en config/)
@Bean
@LoadBalanced
RestClient.Builder loadBalancedRestClientBuilder() {
  return RestClient.builder();
}

@Bean
UserClient userClient(
    @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
  RestClient client = builder.clone()
      .baseUrl(servicesProperties.getMap().get("sgivu-user").getUrl())
      .defaultHeader("X-Internal-Service-Key", internalServiceKey)
      .build();
  return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
      .build().createClient(UserClient.class);
}
```

**Propagación de JWT**: `JwtAuthorizationInterceptor` extrae el token del `SecurityContext` y lo agrega como `Authorization: Bearer` a las peticiones salientes.

**Dos modos de autenticación entre servicios**:

1. **Con contexto de usuario**: Relay del JWT vía interceptor (gateway usa `tokenRelay` filter).
2. **Sin contexto de usuario**: Header `X-Internal-Service-Key` (secreto compartido entre todos los servicios).

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

**Autorización basada en permisos** (no roles): Formato `"recurso:accion"` (e.g., `user:create`, `vehicle:delete`, `car:read`).

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
4. **Todos los Servicios → Config**: Importación desde `configserver:http://sgivu-config:8888`
5. **Todos los Servicios → Discovery**: Registro con Eureka en `http://sgivu-discovery:8761/eureka`

## Frontend (Angular)

Ubicado en `apps/frontend/sgivu-frontend`. Comunica exclusivamente a través del gateway.

Build: `npm run build`
Servidor dev: `npm run start` (puerto 4200)

### Stack y Librerías

- **Angular 21** con standalone components y lazy-loaded routes
- **Bootstrap 5.3** + CSS variables personalizadas para temas light/dark
- **Reactive Forms** con tipado fuerte (`FormGroup<FormControls>`)
- **ESLint + Prettier** configurados
- **Locale**: `es-CO` (español colombiano) registrado globalmente

### Gestión de Estado con Signals

Signals son la **única fuente de verdad** (no NgRx). Patrón en servicios:

```ts
// Signal privado mutable
private _users = signal<User[]>([]);
// Signal público de solo lectura
readonly users = this._users.asReadonly();
// Computed para estado derivado
readonly activeUsers = computed(() => this._users().filter(u => u.enabled));
```

- Usar `toSignal()` para convertir observables, `toObservable()` para lo inverso.
- `ChangeDetectionStrategy.OnPush` **obligatorio** en todos los componentes.

### Autenticación y Autorización

- **BFF Pattern**: `defaultOAuthInterceptor` agrega `withCredentials: true` a todas las peticiones HTTP.
- **Endpoints**: `/auth/session` (info de sesión), `/v1/*` (API protegidas vía gateway).
- **Guards**: `authGuard` (verifica sesión activa) + `permissionGuard` (valida permisos del usuario).
- **Directiva**: `*appHasPermission` para control a nivel de template con lógica AND/OR.
- `PermissionService` aplana la jerarquía User → Roles → Permissions para búsqueda rápida.

### Componentes Compartidos

Carpeta `shared/` contiene 15+ componentes reutilizables: `data-table`, `pager`, `page-header`, `kpi-card`, `form-shell`, `navbar`, `sidebar`, `loading-overlay`, `skeleton`, `empty-state`, entre otros.

### Utilidades

| Archivo                 | Función                                            |
| ----------------------- | -------------------------------------------------- |
| `swal-alert.utils.ts`   | Alertas y diálogos con SweetAlert2                 |
| `list-page-manager.ts`  | Clase reutilizable para estado de listas paginadas |
| `form.utils.ts`         | Helpers de validación                              |
| `filter-query.utils.ts` | Conversión filtros ↔ query params                  |
| `currency.utils.ts`     | Formateo de moneda                                 |

### Validators Personalizados (Frontend)

`lengthValidator`, `noWhitespaceValidator`, `noSpecialCharactersValidator`, `passwordStrengthValidator`.

### Temas

`ThemeService` gestiona preferencia y tema activo (light/dark) vía CSS variables de Bootstrap 5.3. Fuente: Manrope (Google Fonts).

## Servicio ML (FastAPI)

Ubicado en `apps/ml/sgivu-ml`. Python 3.12 con scikit-learn.

### Arquitectura por Capas

```text
app/
├── api/                    # Routers FastAPI, schemas Pydantic, exception handlers
│   ├── dependencies.py     # Inyección de dependencias (get_prediction_service)
│   ├── schemas/            # Modelos request/response
│   └── routers/            # health.py, prediction.py
├── application/
│   └── services/           # PredictionService (lógica de negocio)
├── domain/
│   ├── entities.py         # ModelMetadata, modelos de dominio
│   ├── exceptions.py       # Excepciones de dominio personalizadas
│   └── ports/              # Interfaces de repositorio
├── infrastructure/
│   ├── config.py           # Pydantic Settings v2 (LenientEnvSettingsSource)
│   ├── security/           # Validación JWT vía OIDC discovery, X-Internal-Service-Key
│   ├── ml/                 # Feature engineering, entrenamiento, normalización
│   ├── http/               # Clientes API a otros servicios
│   └── persistence/        # Async SQLAlchemy + asyncpg, Alembic
└── main.py                 # Lifespan manager, exception handlers, routers
```

### Patrones Clave

- **Inyección de dependencias**: FastAPI `Depends()` + factory functions.
- **Configuración**: Pydantic Settings v2 con `LenientEnvSettingsSource` para parseo flexible de variables de entorno.
- **Base de datos**: SQLAlchemy async con `asyncpg`, migraciones Alembic. La DB es opcional (`database_enabled()` check).
- **Seguridad**: Valida JWT vía descubrimiento OIDC (`SGIVU_AUTH_DISCOVERY_URL`). Acepta `X-Internal-Service-Key` para llamadas internas. El claim `rolesAndPermissions` del JWT se usa para extraer permisos.
- **Resolución de permisos en tiempo de importación**: Las dependencias de permisos se resuelven al cargar el módulo, no por petición.

### Endpoints

- `/v1/ml/predict*` — Predicciones (filtros + horizonte + confianza → predicciones con RMSE/MAE/MAPE)
- `/v1/ml/retrain` — Reentrenamiento del modelo
- `/health` — Health check
- Rutas expuestas vía gateway en `/v1/ml/**`

## Rutas del Gateway

El gateway (`GatewayRoutesConfig.java`) define el enrutamiento a microservicios usando balanceo de carga vía Eureka (`lb://`):

| Ruta                                                    | Servicio                   | Filtros                    |
| ------------------------------------------------------- | -------------------------- | -------------------------- |
| `/v1/users/**`, `/v1/roles/**`, `/v1/permissions/**`    | `lb://sgivu-user`          | tokenRelay, circuitBreaker |
| `/v1/persons/**`, `/v1/companies/**`                    | `lb://sgivu-client`        | tokenRelay, circuitBreaker |
| `/v1/vehicles/**`, `/v1/cars/**`, `/v1/motorcycles/**`  | `lb://sgivu-vehicle`       | tokenRelay, circuitBreaker |
| `/v1/purchase-sales/**`                                 | `lb://sgivu-purchase-sale` | tokenRelay, circuitBreaker |
| `/v1/ml/**`                                             | `http://sgivu-ml:8000`     | tokenRelay, circuitBreaker |

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
- **Zipkin**: Trazas distribuidas en `http://localhost:9411` (dev), sampling al 10%
- **Eureka Dashboard**: `http://localhost:8761` para verificar instancias registradas
- **Dev**: Expone todos los endpoints actuator + health details `always`
- **Prod**: Solo `health`, `info`, `prometheus`; health details `never`

## Despliegue (Producción)

Nginx actúa como punto de entrada único (ver `infra/nginx/sites-available/default.conf`):

- Infraestructura de despliegue: **AWS EC2**

- Auth Server (puerto 9000): `/login`, `/oauth2/*`, `/.well-known/*`
- Gateway (puerto 8080): `/v1/*`, `/docs/*`, `/auth/session`
- Frontend: S3 como fallback catch-all

## Patrón Dockerfile

Todos los servicios Java usan el mismo patrón:

```dockerfile
FROM amazoncorretto:25-alpine-jdk
WORKDIR /app
COPY ./target/sgivu-<servicio>-0.0.1-SNAPSHOT.jar sgivu-<servicio>.jar
EXPOSE <puerto>
ENTRYPOINT ["java", "-jar", "sgivu-<servicio>.jar"] 
```

## Documentación (Mintlify)

La documentación del proyecto se gestiona con **Mintlify** en el directorio `docs/`. Configuración en `docs/docs.json`.

```bash
npm i -g mint
mint dev                  # Preview local en puerto 3000
mint broken-links         # Verificar enlaces rotos
```

Estructura de tabs: **Documentación** (general, primeros pasos, funcionalidades, infraestructura, seguridad), **Servicios Backend**, **Aprendizaje Automático**, **Frontend**.

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
