# SGIVU - sgivu-discovery

## Descripción

Eureka Server que centraliza el registro y la ubicación dinámica de los microservicios SGIVU, facilitando balanceo, alta disponibilidad y evitando endpoints estáticos.

## Arquitectura y Rol

- Microservicio Spring Boot 3 / Spring Cloud Netflix Eureka Server.
- Interactúa con `sgivu-config`, `sgivu-gateway` y clientes Eureka (`sgivu-auth`, `sgivu-user`, etc.).
- UI en `http://localhost:8761/` y endpoints REST `/eureka/apps/**` para registro de instancias.
- Obtiene configuración desde Config Server (`configserver:http://sgivu-config:8888`) con perfil `prod`.
- Estado en memoria; sin persistencia.

## Tecnologías

- Lenguaje: Java 21 (Amazon Corretto en Docker)
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0, Netflix Eureka Server
- Seguridad: sin autenticación integrada; operación en red privada detrás de `sgivu-gateway`
- Infraestructura: Docker, AWS (EC2)

## Configuración

- Variables frecuentes: `SPRING_PROFILES_ACTIVE`, `SPRING_CONFIG_IMPORT`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` en clientes.
- Ajusta `SPRING_CONFIG_IMPORT` si el Config Server corre fuera de localhost.

## Ejecución Local

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_CONFIG_IMPORT=configserver:http://localhost:8888
./mvnw spring-boot:run
```

Accede al dashboard en `http://localhost:8761`.

## Endpoints Principales

```text
GET /
GET /eureka/apps
GET /eureka/apps/{applicationName}
```

- `/`: Panel web de Eureka.
- `/eureka/apps`: catálogo completo de instancias registradas.
- `/eureka/apps/{applicationName}`: detalle de instancias por servicio.

## Seguridad

- Actualmente sin OAuth2; corre en red interna. Planificada integración con `sgivu-auth` para asegurar `/eureka/**` mediante JWT y roles de servicio.

## Dependencias

- `sgivu-config` (configuración centralizada).
- `sgivu-gateway` (enrutamiento y protección perimetral).
- Microservicios clientes que se registran en Eureka.

## Dockerización

- Imagen: `sgivu-discovery`
- Puerto expuesto: 8761

Ejemplo:

```bash
./mvnw clean package -DskipTests
docker build -t sgivu-discovery .
docker run --rm -p 8761:8761 \
  -e SPRING_CONFIG_IMPORT=configserver:http://host.docker.internal:8888 \
  sgivu-discovery
```

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-discovery:v1`.
- Orquestadores externos pueden llamarlo al construir todos los servicios.

## Despliegue

- Despliega en EC2 dentro de VPC privada.
- Configura `SPRING_CONFIG_IMPORT` hacia el Config Server gestionado.
- Expón puerto 8761 solo a la subred interna o al balanceador.

## Monitoreo

- Integrable con Micrometer + Prometheus vía Actuator.
- Puede enviar trazas a Zipkin habilitando `spring.zipkin.baseUrl` desde Config Server.
- Salud mínima con `GET /actuator/health` si se incluye Actuator.

## Troubleshooting

- Sin servicios registrados: revisa `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` en clientes y acceso a `http://localhost:8761/eureka`.
- UI vacía intermitente: verifica tiempos de lease y sincronización NTP.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
