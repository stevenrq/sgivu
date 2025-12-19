# Guía del Repositorio

## Estructura del Proyecto y Módulos

- Código principal en `src/main/java/com/sgivu/vehicle` (controllers, services/impl, repositories, entities, DTOs, security, config, specifications, mapper).
- Configuración y semillas SQL en `src/main/resources/application.yml` y `database/{schema,data}.sql`; pruebas en `src/test/java/com/sgivu/vehicle`.
- Diagramas y documentación en `../../../docs/diagrams/**`; activos Docker en `Dockerfile` y `./build-image.bash`.

## Comandos de Build, Pruebas y Desarrollo

- `./mvnw clean package` compila, ejecuta pruebas y genera el JAR.
- `./mvnw test` ciclo rápido de unitarias/integración.
- `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` levanta local apuntando a config/discovery/auth.
- `./build-image.bash` construye y publica la imagen de contenedor (usado por orquestadores).

## Estilo de Código y Convenciones

- Java 21, Spring Boot 3.5; mantén indentación de 2 espacios y llaves en la misma línea.
- Usa inyección por constructor y anota con `@Transactional` los métodos de servicio que mutan estado.
- Identificadores en inglés; comentarios/Javadoc en español. DTOs terminan en `Request`/`Response`; entidades y enums en singular; repositorios terminan en `Repository`.
- Prefiere MapStruct (`VehicleMapper`) para transformaciones; evita copias manuales cuando existe un mapper.
- Valida entrada con anotaciones de `jakarta.validation`; protege endpoints con `@PreAuthorize` usando autoridades existentes (`vehicle:*`, `car:*`, `motorcycle:*`).

## Guía de Pruebas

- Stack: `spring-boot-starter-test` y `spring-security-test`. Replica la ruta de paquetes en `src/test/java`; nombra archivos como `*Tests.java`.
- Prioriza pruebas de corte (`@DataJpaTest`, controladores con `MockMvc`) antes de `@SpringBootTest`; usa semillas de `database/*.sql` en escenarios de integración.
- Ejecuta `./mvnw test` antes de subir cambios y documenta cobertura/riesgos en la descripción del PR.

## Commits y Pull Requests

- Sigue Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`); mensajes concisos y en inglés.
- Los PR deben incluir resumen de alcance, issue vinculado, impactos en API/DB/seguridad y resultados de pruebas locales. Añade ejemplos de consumo (curl/Postman) para nuevos endpoints cuando aporte valor.
- Separa refactors de cambios de comportamiento; evita mezclar ediciones no relacionadas.

## Seguridad y Configuración

- No subas secretos; usa variables de entorno o `application-local.yml` (ignorado). La configuración externa proviene del Config Server; sobreescribe con env al correr en local.
- Verifica conectividad a Config Server, Eureka, Auth y PostgreSQL antes de levantar el servicio. Define bucket/región/credenciales de S3 vía env o Config Server, nunca hardcoded.

## Notas Específicas del Servicio

- Protege endpoints con autoridades acordes (`vehicle:*`, `car:*`, `motorcycle:*`) y mantén consistentes los mapeos de MapStruct cuando cambien los DTOs.
