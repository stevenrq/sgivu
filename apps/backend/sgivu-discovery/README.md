# sgivu-discovery - SGIVU

## Descripción

`sgivu-discovery` es el servidor de descubrimiento de servicios (Eureka Server) del ecosistema **SGIVU**. Su función es registrar y mostrar las instancias de los microservicios, permitir la resolución de nombres de servicio y servir como fuente central para balanceo simple entre servicios.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 3.5.8
- Spring Cloud Netflix Eureka Server
- Spring Cloud Config (cliente)
- Spring Boot Devtools (desarrollo)

## Requisitos Previos

- JDK 21
- Maven 3.9+
- Docker & docker-compose
- Para entorno local, se recomienda lanzar la stack completa (`infra/compose/sgivu-docker-compose/docker-compose.dev.yml`) que incluye `sgivu-config` y otros servicios.

## Arranque y Ejecución

### Desarrollo (docker-compose)

Desde `infra/compose/sgivu-docker-compose`:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Esto arranca `sgivu-config` en modo `native` y `sgivu-discovery` entre otros servicios.

### Ejecución Local

```bash
./mvnw clean package
./mvnw spring-boot:run
```

### Docker

```bash
./build-image.bash
docker build -t stevenrq/sgivu-discovery:v1 .
docker run -p 8761:8761 stevenrq/sgivu-discovery:v1
```

## Endpoints Principales

| Endpoint | Descripción |
| --- | --- |
| `http://<host>:8761/` | Eureka UI (dashboard) - lista todas las aplicaciones registradas |
| `GET /eureka/apps` | Lista todas las apps registradas (API REST) |
| `GET /eureka/apps/{appName}` | Información de una aplicación específica |
| `GET /eureka/apps/{appName}/{instanceId}` | Información de una instancia específica |
| `GET /actuator/health` | Estado de salud del servicio (si Actuator está habilitado) |

Ejemplo:

```bash
curl http://localhost:8761/eureka/apps
```

## Seguridad

- Asegurar que los clientes Eureka (los microservicios) están configurados para apuntar a la URL correcta (`EUREKA_URL` / `eureka.client.service-url.defaultZone`) y que `spring.application.name` sea único por servicio.

## Observabilidad

- El servicio no incluye dependencias de tracing por defecto, pero su funcionamiento puede observarse mediante logs y (si se habilita) Actuator.
- Para trazas distribuidas habilitar Micrometer/Zipkin en los demás servicios y revisar las dependencias/registro en `sgivu-config-repo`.

## Pruebas

```bash
./mvnw test
```

- Test base: `src/test/java/com/sgivu/discovery/DiscoveryApplicationTests.java`
- Recomendación: añadir pruebas de integración que simulen la inscripción de instancias y verifiquen la visibilidad en `/eureka/apps`.

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| Ningún servicio aparece en el dashboard | Verificar que los clientes tengan `eureka.client.service-url.defaultZone` apuntando a `http://sgivu-discovery:8761/eureka/` |
| Error de red | Revisar docker networks y que `sgivu-config` esté accesible |
| Puerto en uso | Comprobar si otro proceso está usando el puerto 8761 |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests para cambios funcionales
