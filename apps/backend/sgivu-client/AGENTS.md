# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `src/main/java/com/sgivu/client`: controladores REST, DTOs, mappers (MapStruct), servicios/impl, repositorios,
  utilidades de seguridad/config/specification/exception.
- `src/main/resources`: `application.yml` y activos de base de datos (`database/schema.sql`) para bootstrap
  local.
- `src/test/java`: suites con JUnit 5 + Mockito + AssertJ; refleja la misma jerarquía de paquetes.
- `../../../docs/diagrams`: diagramas PlantUML (contexto, componentes, modelo de datos).
- Entregables: `Dockerfile`, `build-image.bash`, artefactos Maven en `target/`.

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw clean package` (añade `-DskipTests` si solo empaquetas) genera el jar en `target/`.
- `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` corre en local; puedes sobreescribir valores con
  `application-local.yml` o variables de entorno.
- `./mvnw test` ejecuta la batería de pruebas unitarias.
- `./build-image.bash` recompila y publica la imagen Docker `sgivu-client`; lo invoca el orquestador multi-servicio.
- Docker manual: `docker build -t sgivu-client .` y `docker run --env-file .env -p 8082:8082 sgivu-client`.

## Estilo de Código y Convenciones

- Java 21, Spring Boot 3.5; mantiene indentación de ~2 espacios y ~120 columnas.
- Código en inglés; comentarios y docs en español. Usa Lombok para boilerplate y MapStruct para mapear DTO/entidad.
- Nombres: `*Controller`, `*Service`/`*ServiceImpl`, `*Repository`; DTOs terminados en `Request`/`Response`;
  specifications bajo `specification`; config y seguridad en `config`/`security`; manejadores globales en `exception`.

## Guía de Pruebas

- Stack: JUnit 5, Mockito, AssertJ; anota con `@ExtendWith(MockitoExtension.class)` en pruebas unitarias.
- Nombra por comportamiento y mantén aserciones enfocadas por escenario.
- Cubre ramas de servicio (actualizaciones, paginación, toggles de estado, filtros de búsqueda) y verifica interacciones
  de repositorio con mocks; evita depender de la BD creando entidades de ejemplo.
- Ejecuta `./mvnw test` antes de cada PR; agrega regresiones al tocar controladores, mappers o reglas de seguridad.

## Commits y Pull Requests

- Commits con Conventional Commits en inglés (`feat: add person pagination`, `fix: preserve tax id on update`).
- PRs: resume propósito, lista endpoints/config tocados, enlaza issues, declara variables/env necesarias y comparte
  snippets curl/OpenAPI para cambios de API; asegura build/tests en verde.

## Seguridad y Configuración

- Secretos/config vienen de `sgivu-config` o variables de entorno; no comprometas credenciales. Define
  `SPRING_CONFIG_IMPORT` y `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` por entorno.
- La validación JWT depende de `services.map.sgivu-auth.url`; mantenla correcta en cada perfil. Endpoints públicos
  limitados a actuator health/info.
- Alinea la BD local con `src/main/resources/database/schema.sql`; prefiere properties/feature flags en lugar de valores
  hardcoded para facilitar despliegues.

## Notas Específicas del Servicio

- Revisa que las specifications y filtros sigan alineados con los DTOs y mappers cuando agregues campos nuevos.
