# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `src/main/java/com/sgivu/gateway`: configuración (rutas y circuit breakers), filtros globales (trazabilidad Zipkin, encabezados de seguridad), seguridad y controladores de fallback.
- `src/main/resources/application.yml`: bootstrap mínimo; el resto de propiedades proviene del Config Server.
- `src/test/java/com/sgivu/gateway`: pruebas de filtros, controladores y arranque de contexto.
- `../../../docs/diagrams/...`: diagramas PlantUML de contexto y componentes.
- `pom.xml`, `Dockerfile`, `build-image.bash`: dependencias, empaquetado y publicación de imagen; artefactos en `target/`.

## Comandos de Build, Pruebas y Desarrollo

- Ejecuta en local con Config Server y Eureka activos y variables, por ejemplo:

  ```bash
  export SPRING_PROFILES_ACTIVE=dev
  export SPRING_CLOUD_CONFIG_URI=http://localhost:8888
  export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka
  ./mvnw spring-boot:run
  ```

- `./mvnw test`: pruebas unitarias/reactivas (JUnit 5, AssertJ, Reactor Test, Spring Security Test).
- `./mvnw clean package` o `./mvnw clean package -DskipTests`: empaqueta JAR en `target/`.
- `./build-image.bash`: construye y publica la imagen Docker `sgivu-gateway` (requiere Docker login).

## Estilo de Código y Convenciones

- Java 21 + Spring Boot 3 (WebFlux); evita llamadas bloqueantes en filtros o controladores.
- Sangría de 2 espacios, llaves en la misma línea y métodos concisos con nombres en `camelCase`; clases en `PascalCase`, constantes `UPPER_SNAKE_CASE`.
- Código en inglés; comentarios y documentación en español. Mantén coherencia con el paquete `com.sgivu.gateway` y reutiliza beans existentes.

## Guía de Pruebas

- Nombra las clases de prueba como `*Tests` y ubícalas en el mismo paquete que la clase bajo prueba.
- Usa `@SpringBootTest` para carga completa del contexto y `@WebFluxTest` o intercambiadores simulados (`MockServerWebExchange`, `StepVerifier`) para pruebas de filtros.
- Cubre rutas, fallbacks y lógica de cabeceras (p. ej., propagación de `X-User-ID` y circuit breakers) antes de abrir un PR.

## Commits y Pull Requests

- Mensajes en inglés siguiendo Conventional Commits (`feat: add ml fallback route`, `fix: handle missing subject claim`).
- Incluye en el PR: resumen conciso, issue asociado, pasos de prueba (comandos y resultados), cambios de configuración/variables y capturas o ejemplos de respuesta si modificas los mensajes de fallback.
- Actualiza diagramas en `../../../docs/diagrams` cuando cambie el flujo de ruteo o dependencias con otros servicios.

## Seguridad y Configuración

- **Gestión de Tokens (BFF):** El gateway implementa el patrón BFF para la aplicación Angular, siendo responsable de almacenar y servir los tokens (`access_token` y `refresh_token`) emitidos por `sgivu-auth`.
- No versiones secretos; usa variables de entorno o perfiles remotos del Config Server.
- Verifica que las URLs de `sgivu-auth`, `sgivu-config` y `sgivu-discovery` correspondan al entorno antes de desplegar; revisa los encabezados propagados (`X-User-ID`, `X-Trace-Id`) en logs para trazabilidad.

## Notas Específicas del Servicio

- Evita llamadas bloqueantes en filtros WebFlux y valida propagación de headers (`X-User-ID`, `X-Trace-Id`).
