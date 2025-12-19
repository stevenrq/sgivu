# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `src/main/java/com/sgivu/purchasesale/**`: controladores, servicios, clientes externos, mapeadores (MapStruct), entidades, repositorios y configuración de seguridad; `PurchaseSaleApplication.java` es el punto de entrada.
- `src/main/resources/application.yaml`: configuración base; sobrescribe con `application-local.yml` o variables de entorno. SQL en `src/main/resources/database/` (`schema.sql`, `data.sql`, `queries.sql`).
- `src/test/java/com/sgivu/purchasesale/**`: pruebas con JUnit 5, Mockito y AssertJ; fixtures cerca del código bajo prueba.
- Herramientas: `Dockerfile` para la imagen, `build-image.bash` para automatizar build/push y `README.md` como guía operativa.

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw clean package` ejecuta build completo con pruebas.
- `./mvnw test` ejecuta solo pruebas.
- `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` corre en local (requiere Config Server, Eureka, Auth y base de datos); o `java -jar target/sgivu-purchase-sale-0.0.1-SNAPSHOT.jar`.
- `./mvnw clean package -DskipTests` para iteración rápida.
- `./build-image.bash` limpia, recompila y publica la imagen usada por los scripts orquestadores.

## Estilo de Código y Convenciones

- Java 21 / Spring Boot 3.5. Identificadores en inglés; comentarios y mensajes pueden mantenerse en español.
- Sangría de 2 espacios, llaves estilo K&R, sin tabs. Prefiere inyección por constructor y campos `final`.
- DTOs siguen `*Request`, `*Response`, `*DetailResponse`; enums en mayúsculas con guiones bajos. Usa MapStruct cuando exista mapeador en vez de conversión manual.
- Lombok para boilerplate (`@Data`, `@Builder`); métodos explícitos cuando haya lógica.

## Guía de Pruebas

- Usa JUnit 5 + Mockito + AssertJ; las clases terminan en `Test` o `Tests` y reflejan el paquete fuente.
- Mockea clientes (`client`, `vehicle`, `user`) y repositorios; valida reglas (transiciones de estado, precios por defecto, requisitos de auth) en lugar de wiring de Spring.
- Agrega pruebas de regresión al modificar validaciones, specifications o anotaciones de seguridad; ejecuta `./mvnw test` antes del PR.

## Commits y Pull Requests

- Commits con Conventional Commits en inglés (`feat`, `fix`, `chore`, `test`, `docs`, `refactor`); mantén cada commit enfocado.
- PRs: resumen breve, issue vinculada, nota de pruebas y resalta cambios de API/SQL/config (`SERVICE_INTERNAL_SECRET_KEY`, `SPRING_DATASOURCE_*`, `SPRING_PROFILES_ACTIVE`).
- Si cambian contratos o responses, adjunta un request/response de ejemplo o lista los DTOs afectados.

## Seguridad y Configuración

- No subas secretos ni tokens; cárgalos vía variables de entorno o un gestor de secretos.
- Usa `application-local.yml` (ignorado por Git) para overrides locales; ejecuta Config Server, Eureka y Auth en dev para simular integración real.
- Flujos internos exigen el header `X-Internal-Service-Key`; endpoints tras gateway requieren JWT con claim `rolesAndPermissions`.

## Notas Específicas del Servicio

- Verifica transiciones de estado y pricing por defecto en pruebas cuando cambien reglas de negocio o mapeos de DTOs.
