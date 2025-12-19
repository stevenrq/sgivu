# SGIVU - documentación del sistema

## Descripción

Documentación central del sistema SGIVU (backend, frontend, ML e infraestructura) con guías de contribución, arquitectura, orquestación y scripts auxiliares.

## Arquitectura y Rol

- Índice general de documentación para todos los componentes del sistema.
- Referencia a diagramas PlantUML y scripts de validación.
- Enlaces a documentación específica: `infra/compose/sgivu-docker-compose/README.md`, `apps/backend/*/README.md`, `apps/frontend/sgivu-frontend/README.md`, `apps/ml/sgivu-ml/README.md`.

![Arquitectura general de SGIVU](docs/diagrams/img/01-system-architecture.png)

## Tecnologías

- Backend: Spring Boot, Spring Cloud, PostgreSQL.
- Frontend: Angular.
- ML: FastAPI, scikit-learn.
- Infraestructura: Docker, Docker Compose, AWS.
- Observabilidad: Actuator, Micrometer, Zipkin.

## Configuración

- Configuración centralizada en `sgivu-config` y repositorio Git de configuración.
- Variables de entorno base en `infra/compose/sgivu-docker-compose/.env.example`.
- Versiones documentadas validadas con `scripts/check-readme-boot-version.sh`.

## Ejecución Local

- Stack completo con Docker Compose: `infra/compose/sgivu-docker-compose/run.bash --dev`.
- Alternativa: ejecutar cada servicio siguiendo su README.
- Validar versiones: `./scripts/check-readme-boot-version.sh`.

## Endpoints Principales

- Gateway: `http://localhost:8080`
- Auth: `http://localhost:9000`
- Config: `http://localhost:8888`
- Discovery: `http://localhost:8761`
- Frontend: `http://localhost:4200`
- ML: `http://localhost:8000`
- Zipkin (opcional): `http://localhost:9411`

## Seguridad

- OAuth 2.1/OIDC con JWT emitidos por `sgivu-auth`.
- Claves internas para comunicación service-to-service.
- Nunca versionar secretos ni `.env` reales.

## Dependencias

- Orquestación local: `infra/compose/sgivu-docker-compose/README.md`.
- Backend: `apps/backend/*/README.md`.
- Frontend: `apps/frontend/sgivu-frontend/README.md`.
- ML: `apps/ml/sgivu-ml/README.md`.

## Dockerización

- Cada servicio cuenta con `Dockerfile` y scripts `build-image.bash` cuando aplica.
- Stack integrado vía Docker Compose en `infra/compose/sgivu-docker-compose`.

## Build y Push Docker

- Orquestador: `infra/compose/sgivu-docker-compose/build-and-push-images.bash`.
- Servicios individuales: `apps/**/build-image.bash` cuando existe.

## Despliegue

- Infra sugerida: VPC privada con EC2/ECS/EKS, RDS y ALB.
- Exponer públicamente solo el gateway; el resto en red interna.

## Monitoreo

- Actuator en servicios Spring y health checks en FastAPI.
- Trazas y métricas vía Zipkin/Prometheus si están habilitados.

## Troubleshooting

- Puertos ocupados: revisa mapeos en Compose y detén procesos locales.
- Config Server inaccesible: valida `SPRING_CONFIG_IMPORT` o `SPRING_CLOUD_CONFIG_URI`.
- Mismatch de versiones: ejecuta `./scripts/check-readme-boot-version.sh`.

## Buenas Prácticas y Convenciones

- Código en inglés; documentación en español; commits en inglés con Conventional Commits.
- Mantener README y diagramas sincronizados con cambios de puertos o flujos.

## Diagramas

- Arquitectura general: `docs/diagrams/01-system-architecture.puml`.
- Pipeline de build: `docs/diagrams/02-build-pipeline.puml`.

## Autor

- Steven Ricardo Quiñones (2025)
