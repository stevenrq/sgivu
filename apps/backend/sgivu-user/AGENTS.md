# Guía del Repositorio

## Estructura del Proyecto y Módulos

- Código en `src/main/java/com/sgivu/user` organizado por responsabilidad: `controller`, `service` y `service/impl`,
  `repository`, `mapper` (MapStruct), `security`, `dto`, `entity`, `validation` y `exception`.
- SQL comunes en `src/main/resources/database/{schema.sql}`; configuración base en
  `src/main/resources/application.yml`.
- Pruebas en `src/test/java/com/sgivu/user` con utilidades compartidas en `ServiceTestDataProvider`.
- Diagramas en `../../../docs/diagrams/...`; activos Docker: `Dockerfile` y `build-image.bash`.

## Comandos de Build, Pruebas y Desarrollo

Usa siempre los wrappers de Maven:

```bash
./mvnw clean verify
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
./mvnw clean package
./build-image.bash
```

Para correr local: Config Server, Eureka y PostgreSQL activos (ver variables en README). Si solo necesitas imagen local:
`docker build -t sgivu-user .`.

## Estilo de Código y Convenciones

- Java 21 + Spring Boot 3.5; código en inglés, comentarios en español.
- Sangría de 2 espacios, inyección por constructor y evita inyección en campos.
- DTOs terminan en `Requst`/`Response`; entidades usan Lombok; MapStruct vive en `mapper`.
- Nombra métodos de forma explícita (`findByUsername`, `changeStatus`); controladores delgados, lógica en servicios y
  validaciones en `validation`.

## Guía de Pruebas

- Frameworks: JUnit 5, Spring Boot Test y `spring-security-test`.
- Ejecuta `./mvnw test` o `./mvnw verify` antes de enviar cambios.
- Nombra clases `*Tests` y métodos con intención clara (`should...`).
- Reutiliza fixtures de `ServiceTestDataProvider`; en controladores usa `@WebMvcTest`/MockMvc con matchers de seguridad;
  en servicios, mockea repositorios y cubre ramas de autorización/validación.

## Commits y Pull Requests

- Commits en inglés siguiendo Conventional Commits (`feat: add paginated user search`,
  `fix: handle missing role permissions`).
- PRs: describe el cambio, enlaza el ticket, lista pruebas ejecutadas e impactos en config/entorno (nuevas propiedades o
  SQL). Actualiza README o `../../../docs/diagrams` cuando cambien endpoints, roles/permisos o esquema de base de datos.

## Seguridad y Configuración

- No comprometas secretos; cárgalos vía Config Server o variables (`SERVICE_INTERNAL_SECRET_KEY`, credenciales DB,
  issuer URLs`).
- Para overrides locales, usa `SPRING_PROFILES_ACTIVE=dev` con datasource consistente y evita modificar
  `application.yml` base.
- Mantén guardas `@PreAuthorize` y validaciones de headers (`X-Internal-Service-Key`, `X-User-ID`) en endpoints nuevos,
  y mapea DTO sensibles a respuestas seguras con MapStruct.

## Notas Específicas del Servicio

- Alinea seeds SQL con expectativas de pruebas y valida que los permisos/roles estén reflejados en endpoints y DTOs.
