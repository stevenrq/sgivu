# sgivu-config - SGIVU

## Descripción

`sgivu-config` es el servidor de configuración central (Spring Cloud Config Server) del ecosistema **SGIVU**. Exponiendo configuración centralizada desde un repositorio Git o desde el sistema de archivos (modo *native*), el servicio permite que los microservicios obtengan propiedades según aplicación/perfil/label.

## Tecnologías y Dependencias

- Java 21
- Spring Boot 3.5.8
- Spring Cloud Config Server (2025.x)
- Spring Boot Actuator

## Requisitos Previos

- JDK 21
- Maven 3.9+
- Docker & docker-compose (opcional; `infra/compose/sgivu-docker-compose` ya incluye el servicio)
- Acceso al repositorio de configuración `https://github.com/stevenrq/sgivu-config-repo.git` (o disponer de una copia local para `native`)

## Arranque y Ejecución

### Desarrollo (docker-compose)

Desde `infra/compose/sgivu-docker-compose`:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Esto arranca `sgivu-config` en modo `native` con el repo local montado.

### Ejecución Local

```bash
./mvnw clean package
./mvnw spring-boot:run
```

### Docker

```bash
./build-image.bash
docker run -p 8888:8888 --env-file .env stevenrq/sgivu-config:v1
```

### Producción (modo `git`)

Por defecto `src/main/resources/application.yml` configura `spring.profiles.active: git` y el `git.uri` apuntando a `https://github.com/stevenrq/sgivu-config-repo.git`.

## Endpoints Principales

| Endpoint | Descripción |
| --- | --- |
| `GET /{application}/{profile}` | Propiedades para una aplicación y perfil |
| `GET /{application}/{profile}/{label}` | Propiedades con label específico (branch) |
| `GET /{application}-{profile}.yml` | Archivo YAML de configuración |
| `GET /actuator/health` | Estado de salud del servicio |

Ejemplo:

```bash
curl http://localhost:8888/sgivu-auth/dev
```

## Seguridad

- En desarrollo, se monta el repo local (native) para comodidad. En producción, se usa el repo Git remoto.
- Asegurar que el repositorio Git es accesible y seguro (usar credenciales si es privado).

## Observabilidad

- **Actuator:** `health` en `http://localhost:8888/actuator/health`.
- Monitorizar actividad de requests y acceso a endpoints `/env`, `/config` según necesidad.

## Pruebas

```bash
./mvnw test
```

- Test base: `src/test/java/com/sgivu/config/ConfigApplicationTests.java`
- Recomendación: añadir tests que verifiquen carga desde Git y desde `native` para detectar fallos de parsing de propiedades.

## Solución de Problemas

| Problema | Solución |
| --- | --- |
| `git uri` inaccesible | Revisar `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` y permisos/credenciales |
| Cambios no reflejados | Verificar `label` y branch; usar `/?label=...` o reiniciar el servidor si se usa `native` |

## Contribuciones

1. Fork → branch → PR
2. Añadir tests cuando cambies comportamiento
