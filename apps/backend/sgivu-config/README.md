# SGIVU - sgivu-config

## Descripción

Servicio Spring Cloud Config que centraliza y expone la configuración del ecosistema SGIVU. Provee archivos de configuración versionados en Git para que los microservicios consuman parámetros consistentes en cada ambiente.

## Arquitectura y Rol

- Microservicio Spring Boot 3 / Spring Cloud Config Server.
- Interactúa con `sgivu-config-repo` (Git), `sgivu-discovery`, `sgivu-gateway` y los microservicios consumidores.
- Expone endpoints REST para entregar propiedades externas; no persiste datos propios.
- Puede registrarse en Eureka si se habilita el cliente Discovery.

## Tecnologías

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.8, Spring Cloud 2025.0.0 (Leyden)
- Seguridad: Actuator sin autenticación por defecto
- Infraestructura: Docker, AWS (EC2)

## Configuración

- Variables clave: `SPRING_PROFILES_ACTIVE` (git por defecto), `SPRING_CLOUD_CONFIG_SERVER_GIT_URI`, `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL`.
- Ajusta `src/main/resources/application.yml` o variables de entorno según el repositorio de configuración.

## Ejecución Local

```bash
./mvnw spring-boot:run
```

Accede a `http://localhost:8888` y consume los endpoints del Config Server.

## Endpoints Principales

```text
GET /{application}/{profile}
GET /{application}/{profile}/{label}
GET /actuator/health
```

- `/{application}/{profile}`: propiedades para la aplicación y perfil solicitados.
- `/{application}/{profile}/{label}`: permite apuntar a una rama o etiqueta específica.
- `/actuator/health`: estado básico del Config Server.

## Seguridad

- Expuesto sin autenticación por defecto. Se recomienda proteger vía `sgivu-gateway` o habilitar Spring Security integrado con `sgivu-auth` (OAuth2 + JWT).

## Dependencias

- `sgivu-config-repo` como backend Git.
- `sgivu-discovery` opcional si se registra para descubrimiento.
- `sgivu-gateway` como proxy que puede asegurar y enrutar el acceso.
- Microservicios SGIVU consumidores de configuración externa.

## Dockerización

- Imagen: `sgivu-config`
- Puerto expuesto: 8888

Ejemplo:

```bash
docker build -t sgivu-config .
docker run -p 8888:8888 \
  -e SPRING_CLOUD_CONFIG_SERVER_GIT_URI=https://github.com/stevenrq/sgivu-config-repo.git \
  sgivu-config
```

## Build y Push Docker

- `./build-image.bash` detiene/borrar contenedores previos, empaqueta con Maven y publica `stevenrq/sgivu-config:v1`.
- Orquestadores externos (`build_push_all.bash`) pueden invocarlo automáticamente.

## Despliegue

- Provisiona EC2 con acceso al repositorio Git, Java 21 y Docker.
- Configura variables `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y credenciales si el repo es privado.
- Ajusta `SERVER_PORT` si cambias el puerto expuesto.

## Monitoreo

- Actuator expone salud y métricas básicas (`/actuator/*`).
- Puede integrarse con Micrometer hacia Prometheus.
- Sleuth/Zipkin se habilita en los clientes que consumen configuración.

## Troubleshooting

- No levanta en 8888: revisa `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y acceso al repositorio (credenciales, etiqueta).
- Servicios sin propiedades: confirma `SPRING_CONFIG_IMPORT=configserver:http://sgivu-config:8888` y prueba `/{application}/{profile}` con curl.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.
- Usa `default` para valores comunes y sobrescribe solo lo necesario en `dev` o `prod`.

## Diagramas

- Arquitectura general: ../../../docs/diagrams/01-system-architecture.puml

## Autor

- Steven Ricardo Quiñones (2025)
