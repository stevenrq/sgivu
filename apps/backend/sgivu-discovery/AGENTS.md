# Guía del Repositorio

Documento para colaborar en el servicio SGIVU Discovery (Eureka). Mantén cambios pequeños, repetibles y alineados con los diagramas de `../../../docs/diagrams`.

## Estructura del Proyecto y Módulos

- `src/main/java/com/sgivu/discovery`: punto de entrada Spring Boot 3.5 (`DiscoveryApplication`) que expone el servidor Eureka; ubica aquí configuración adicional.
- `src/main/resources/application.yml`: propiedades base; la configuración real llega vía `SPRING_CONFIG_IMPORT` (por defecto `configserver:http://sgivu-config:8888`). No subir secretos.
- `src/test/java/com/sgivu/discovery`: pruebas JUnit 5; refleja los paquetes de producción y mantiene fixtures ligeros.
- `../../../docs/diagrams/**`: diagramas PlantUML (contexto, componentes, modelo de datos); actualízalos al cambiar puertos, endpoints o dependencias.
- Activos de contenedor: `Dockerfile`, `build-image.bash` (construye/publica `stevenrq/sgivu-discovery:v1`) y el orquestador externo `../../../infra/compose/sgivu-docker-compose/build-and-push-images.bash`.

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw spring-boot:run`: levanta local con devtools; espera Config Server en `http://localhost:8888` salvo que se sobrescriba `SPRING_CONFIG_IMPORT`.
- `./mvnw test`: ejecuta la suite JUnit 5.
- `./mvnw clean package -DskipTests`: genera el JAR ejecutable en `target/`.
- `./build-image.bash`: limpia contenedor/imagen previa, empaqueta con Maven y construye/empuja la imagen Docker.
- Prueba rápida de imagen: `docker run --rm -p 8761:8761 -e SPRING_CONFIG_IMPORT=configserver:http://host.docker.internal:8888 sgivu-discovery`.

## Estilo de Código y Convenciones

- Java 21, indentado con espacios de 2, llaves en la misma línea y sin imports comodín.
- Código e identificadores en inglés; comentarios y docs pueden ir en español.
- Paquete único `com.sgivu.discovery`; clases en PascalCase, configuraciones con sufijo `Config` o `Properties`; claves YAML en kebab-case.
- Prefiere configurar vía Config Server o variables de entorno antes que literales; logs breves y útiles.

## Guía de Pruebas

- Usa JUnit 5 (`spring-boot-starter-test`); prioriza pruebas rápidas y aisladas.
- Nombra las clases de prueba como `*Tests` junto a la clase objetivo; métodos `should...` o `when...`.
- Para pruebas con contexto Spring, aplica `@SpringBootTest` con el mínimo de beans y simula externos con `SPRING_CONFIG_IMPORT=optional:...`.
- Agrega pruebas al modificar propiedades Eureka, perfiles o comportamiento de arranque; verifica `./mvnw test` antes de subir cambios.

## Commits y Pull Requests

- Sigue Conventional Commits en inglés (`feat: add health checks`, `fix: handle missing config import`, `chore: build image script`).
- Commits acotados; incluye ajustes de config o diagramas cuando cambie el comportamiento.
- PRs: resumen breve, issue vinculada, resultados de pruebas (`./mvnw test` y cualquier `docker run` manual) y nota sobre impactos de configuración.

## Seguridad y Configuración

- No subir credenciales; usa Config Server o variables de entorno para secretos.
- El perfil por defecto es `prod` desde Config Server; sobrescribe localmente con `SPRING_PROFILES_ACTIVE` y `SPRING_CONFIG_IMPORT`.
- Expón el puerto 8761 solo en red interna; asegúrate de que los clientes Eureka apunten a `eureka.client.serviceUrl.defaultZone` correcto.

## Notas Específicas del Servicio

- Ajusta `eureka.client.serviceUrl.defaultZone` por entorno y valida que el dashboard no quede expuesto públicamente.
