# SGIVU - sgivu-auth

## Descripción

Microservicio de autenticación centralizada. Expone un Authorization Server OAuth 2.1/OIDC, gestiona el formulario de inicio de sesión y emite tokens JWT con roles y permisos del servicio de usuarios.

## Arquitectura y Rol

- Microservicio Spring Boot / Spring Cloud.
- Interactúa con `sgivu-config`, `sgivu-discovery`, `sgivu-gateway` y `sgivu-user` para validar credenciales.
- Publica endpoints OAuth 2.1/OIDC (`/oauth2/*`, `/.well-known/*`) y vistas Thymeleaf de login.
- Registra instancias en Eureka y se balancea vía gateway.
- Obtiene configuración sensible (datasource, issuer, secretos) desde Config Server y persiste clientes/autorizaciones en PostgreSQL.

## Tecnologías

- Lenguaje: Java 21 (Amazon Corretto)
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0
- Seguridad: Spring Authorization Server, OAuth 2.1, OIDC, JWT firmados con JKS
- Persistencia: Spring Data JPA + PostgreSQL
- Resiliencia y observabilidad: Resilience4J, Micrometer Tracing (Brave), Zipkin
- Infraestructura: Docker, AWS (EC2, RDS, S3)

## Configuración

- Variables clave: `SPRING_CONFIG_IMPORT`, `SPRING_PROFILES_ACTIVE`, `SERVICE_INTERNAL_SECRET_KEY`, `issuer.url`, `gateway-client.url`, `gateway-client.secret` y propiedades de datasource/keystore (`KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`).
- `application-local.yml` recomendado para desarrollo con placeholders y sin secretos versionados.

## Ejecución Local

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Requiere `sgivu-config`, `sgivu-discovery`, `sgivu-user` y PostgreSQL disponibles (ej. `jdbc:postgresql://localhost:5432/sgivu_auth`). Accede a `http://localhost:9000/login`.

## Endpoints Principales

```text
GET  /.well-known/openid-configuration
GET  /.well-known/jwks.json
GET  /oauth2/authorize
POST /oauth2/token
GET  /oauth2/jwks
GET  /login
POST /api/validate-credentials
GET  /actuator/health
```

- Descubrimiento OIDC y JWKS para clientes.
- Authorization Code y Client Credentials en `/oauth2/*`.
- Vista de login corporativo en `/login`.
- Sondeo de salud en `/actuator/health`.

## Seguridad

- **Integración con BFF:** Emite tokens (`access_token`, `refresh_token`) destinados al cliente confidencial del gateway. `sgivu-gateway` actúa como BFF encargado de almacenar y servir estos tokens a la aplicación Angular.
- OAuth 2.1 Authorization Code y Client Credentials con Spring Authorization Server.
- Emite refresh tokens rotativos para el cliente confidencial del gateway (BFF).
- Los clientes públicos (Angular) usan Authorization Code + PKCE sin refresh tokens.
- Llama a `sgivu-user` con `X-Internal-Service-Key` para validar credenciales y roles/permisos.
- JWT con claims `sub`, `username`, `rolesAndPermissions`, `isAdmin`; firmados con la clave RSA definida en `sgivu.jwt`.

## Dependencias

- `sgivu-config` (configuración externa, secretos JWT, credenciales JDBC).
- `sgivu-discovery` (registro/balanceo).
- `sgivu-gateway` (punto público para `/oauth2/*`).
- `sgivu-user` (autenticación y claims).
- PostgreSQL (local o RDS) para clientes y tokens.

## Dockerización

- Imagen: `sgivu-auth`
- Puerto expuesto: 9000/tcp

Ejemplo:

```bash
docker build -t sgivu-auth .

  -p 9000:9000 \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env SPRING_CONFIG_IMPORT=configserver:http://sgivu-config:8888 \
  sgivu-auth
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-auth:v1`.
- Orquestadores externos pueden invocarlo al construir todos los servicios.

## Despliegue

- Publica la imagen en ECR y despliega en EC2/ECS dentro de la VPC SGIVU.
- Inyecta secretos (`service.internal.secret-key`, credenciales JDBC, propiedades JWT) vía Secrets Manager/Parameter Store.
- Conecta a RDS PostgreSQL con TLS y enruta vía ALB hacia el gateway; evita exponer directamente el puerto 9000.

## Monitoreo

- Actuator expone `health`, `metrics`, `prometheus`.
- Micrometer Tracing + Brave envía spans a Zipkin (`spring.zipkin.*`).
- Circuito `userServiceCircuitBreaker` emite métricas `resilience4j.circuitbreaker.*`.

## Troubleshooting

- Error JDBC: revisa `spring.datasource.*` y conectividad a PostgreSQL.
- JWT inválido: verifica issuer y sincronización de reloj.
- Keystore no encontrado/contraseña inválida: valida `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS` y ruta `keystore.jks`.
- Llamada a sgivu-user rechazada: confirma header `X-Internal-Service-Key` y valor de `SERVICE_INTERNAL_SECRET_KEY`.
- No aparece en Eureka: revisa `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` y `SPRING_CONFIG_IMPORT`.
- Acceso desde host: si el navegador no resuelve `sgivu-auth`, revisa `sgivu-auth-access.md`.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
