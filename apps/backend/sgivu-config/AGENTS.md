# Guía del Repositorio

## Estructura del Proyecto y Módulos

- Código principal en `src/main/java/com/sgivu/config`; `ConfigApplication` inicia el Config Server.
- Configuración por defecto en `src/main/resources/application.yml`; overrides por ambiente en el repo Git externo (`SPRING_CLOUD_CONFIG_SERVER_GIT_URI`).
- Pruebas JUnit 5 en `src/test/java/com/sgivu/config`; extiende el placeholder `ConfigApplicationTests`.
- Diagramas PlantUML en `../../../docs/diagrams/**` (contexto, componentes y flujo de datos).
- Docker y empaquetado: `Dockerfile`, `build-image.bash`; artefactos generados en `target/` (ignorado).

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw spring-boot:run` inicia en el puerto 8888; define `SPRING_PROFILES_ACTIVE` (soporta `git` y `native`) y la URI del repo Git o ruta local por variables de entorno.
- Usa el perfil `native` (`SPRING_PROFILES_ACTIVE=native`) para desarrollo local cargando archivos desde `../sgivu-config-repo` u otra ruta definida en `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS`. En Docker, mapea el volumen a `/config-repo`.
- `./mvnw test` ejecuta la suite JUnit; obligatorio antes de cualquier PR.
- `./mvnw package` genera `target/sgivu-config-0.0.1-SNAPSHOT.jar`.
- `./build-image.bash` construye y publica la imagen Docker; para local usa `docker build -t sgivu-config .` y `docker run -p 8888:8888 -e SPRING_CLOUD_CONFIG_SERVER_GIT_URI=... sgivu-config`.

## Estilo de Código y Convenciones

- Java 21, Spring Boot 3.5.8; sangría de 2 espacios y llaves en la misma línea de la declaración.
- Código en inglés; comentarios y docs en español cuando aporte claridad.
- Paquetes bajo `com.sgivu.config`; clases en PascalCase, métodos/variables en camelCase, constantes en UPPER_SNAKE_CASE.
- Configura mediante `application.yml` o variables de entorno; evita URIs o secretos hardcodeados.

## Guía de Pruebas

- Usa JUnit 5 con el starter de Spring Boot; prefiere slices ligeros o MockMvc para validar endpoints.
- Nombra clases de prueba `*Tests` y métodos con intención (ej.: `shouldServePropertiesFromGitRepo`).
- Agrega regresión al tocar el arranque, la conexión al repo Git o la exposición de actuator; usa stubs/repos locales para aislar dependencias externas.

## Commits y Pull Requests

- Commits en inglés con Conventional Commits (`feat:`, `fix:`, `chore:`, `build:`); cambios pequeños y coherentes.
- Las PR deben describir propósito, issue/tarea ligada, variables de config afectadas y resultados (`./mvnw test` y curl a `/{application}/{profile}`).
- Actualiza docs/diagramas si cambian endpoints, puertos o fuente de configuración; incluye ejemplo cuando aplique.
- No subas `target/` ni credenciales; respeta `.gitignore`.

## Seguridad y Configuración

- Actuator está sin autenticación por defecto; restringe vía gateway o Spring Security antes de producción.
- No expongas secretos; usa variables de entorno y credenciales seguras para el repo de configuración.
- Para pruebas usa un repo/branch desechable (`SPRING_CLOUD_CONFIG_SERVER_GIT_URI`, `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL`) para no contaminar configuraciones compartidas.

## Notas Específicas del Servicio

- Define `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL` en despliegues para apuntar a la rama correcta sin hardcodear URIs.
