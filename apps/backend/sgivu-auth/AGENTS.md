# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `src/main/java/com/sgivu/auth/config/SessionConfig.java`: configuración de cookies para Spring Session JDBC (`AUTH_SESSION`, `SameSite=Lax`).
- `src/main/java/com/sgivu/auth/...`: código Spring Boot (config, controladores, seguridad, servicios, repositorios, DTOs, entidades).
- `src/main/resources`: configuración y assets; crea `application-local.yml` para secretos/DB locales, sin versionar.
- `src/test/java/com/sgivu/auth`: pruebas JUnit/Spring Boot reflejando los paquetes principales.
- `../../../docs/diagrams/...`: diagramas PlantUML (contexto, componentes, modelo de datos).
- Raíz: `pom.xml` (build Maven), `build-image.bash` (build/push Docker), `Dockerfile`, `README.md`.

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw clean verify`: compila y ejecuta pruebas unitarias/integración; úsalo antes de abrir un PR.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`: arranque local; espera Config Server/DB accesibles o overrides locales.
- `./mvnw clean package -DskipTests`: empaqueta el JAR para contenedores.
- `./build-image.bash`: reconstruye y sube la imagen Docker `sgivu-auth` (requiere login/permisos de registro).
- Suites focalizadas: `./mvnw -Dtest=NombreClaseTests test` ejecuta una clase de prueba específica.

## Estilo de Código y Convenciones

- Java 21, Spring Boot 3.5.x; indentación de 2 espacios, archivos UTF-8, sin espacios finales.
- Paquetes en minúsculas; clases/interfaces en PascalCase; métodos/campos en lowerCamelCase; constantes en UPPER_SNAKE.
- Controladores exponen endpoints en inglés; lógica en servicios; DTOs favorecen inmutabilidad con Lombok cuando aplique.
- Código fuente en inglés; comentarios/documentación en español.

## Guía de Pruebas

- JUnit 5 + Spring Boot Test + Spring Security Test; nombra archivos `*Tests.java` junto al paquete probado.
- Cubre lógica de servicios, filtros de seguridad, flujos de autenticación y casos de error (401/403/4xx) con MockMvc/WebTestClient.
- Mockea dependencias externas (`sgivu-user`, Config Server) para evitar I/O real; no uses Postgres real en unit tests.
- Añade pruebas con cada feature/fix y ejecuta `./mvnw clean verify` antes de mergear.

## Commits y Pull Requests

- Usa Conventional Commits (ej.: `feat: agregar endpoint oauth client`, `fix: manejar secret invalido`, `chore: actualizar deps`).
- PRs: resumen breve, issue/ticket vinculado, ejemplos curl o capturas de cambios visibles, impactos en config/vars de entorno y evidencia de pruebas (`./mvnw clean verify` o suite focalizada).
- Mantén commits pequeños y acotados; nunca subas secretos, keystores o `application-local.yml`.

## Seguridad y Configuración

- **Soporte BFF:** El servicio emite tokens de acceso y refresco para ser gestionados por `sgivu-gateway` (BFF), que es el encargado de almacenarlos y servirlos al frontend Angular.
- **Sesiones JDBC:** Las sesiones del formulario de login se persisten en PostgreSQL (`SPRING_SESSION`) con cookie `AUTH_SESSION` (diferente de `SESSION` del Gateway para evitar conflictos).
- **Cookies detrás de Nginx:** La configuración en `SessionConfig.java` establece `SameSite=Lax` y `HttpOnly=true` para compatibilidad con el reverse proxy.
- Externaliza valores sensibles (`SERVICE_INTERNAL_SECRET_KEY`, contraseñas de keystore, credenciales DB) vía variables de entorno o Config Server; nunca los hardcodes.
- Para local, crea `src/main/resources/application-local.yml` con placeholders de DB/issuer y actívalo con `-Dspring.profiles.active=local` o `dev`.
- Los keystores deben proveerse en runtime mediante montajes seguros o secret managers, no en Git.

## Notas Específicas del Servicio

- Endpoints deben mantenerse en inglés; documenta headers requeridos y flujos OAuth/JWT en README cuando cambien.
