# Guía de Contribución — Backend SGIVU

Gracias por contribuir al ecosistema SGIVU. Esta guía resume reglas, flujos y buenas prácticas para mantener la calidad y consistencia entre repositorios y servicios.

## Convenciones base

- Código fuente: inglés.
- Comentarios y documentación: español.
- Commits: inglés, usando Conventional Commits.

Ejemplos de commits válidos:

```text
feat(auth): add PKCE flow to login page
fix(vehicle): correct enum mapping for VehicleStatus
refactor(gateway): extract JWT converter into component
docs: update compose troubleshooting and diagrams index
chore(ci): validate README Spring Boot version matches pom.xml
```

## Rama y PRs

- Rama principal: `main` (estable). Desarrollo: `develop`.
- Crea ramas desde `develop`: `feature/<corto-descriptivo>`, `fix/<corto-descriptivo>`.
- Abre PR contra `develop`. Describe propósito, alcance, riesgos, comandos ejecutados y resultados.
- Small PRs > big PRs. Prefiere cambios atómicos y bien descritos.

Checklist de PR (mínimo):

- [ ] `./mvnw test` verde en servicios Java tocados.
- [ ] `npm test` verde en `sgivu-frontend` si aplica.
- [ ] `python3 -m pip install -r requirements.txt && ./run.sh` valida arranque en `sgivu-ml` si aplica.
- [ ] README(s) y diagramas actualizados cuando afectan comportamiento o configuración.
- [ ] Sin secretos agregados; usar placeholders/variables.
- [ ] Versiones en README (Spring Boot) alineadas con `pom.xml`.
- [ ] `docker compose -f infra/compose/sgivu-docker-compose/docker-compose.dev.yml config` sin errores.

## Calidad y pruebas

Java (Spring Boot 3.5.8 / Java 21)

```bash
./mvnw clean package         # build completo
./mvnw test                  # unit + slice tests
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Python (sgivu-ml)

```bash
python3 -m pip install -r requirements.txt
./run.sh
```

Angular (sgivu-frontend)

```bash
npm install
npm test
npm start
```

Estilo y estructura

- Java: constructor injection, 2 espacios, paquetes por capa (controller, service, repository, etc.).
- Python: tipado estático cuando sea posible, docstrings en español.
- Angular: standalone components, Prettier (2 espacios), servicios para lógica de negocio.

## Seguridad

- Nunca commitees secretos. Usa variables/env y Config Server.
- Usa [infra/compose/sgivu-docker-compose/.env.example](infra/compose/sgivu-docker-compose/.env.example) como plantilla.
- Encapsula claves internas en `SERVICE_INTERNAL_SECRET_KEY` y headers `X-Internal-Service-Key` solo en llamadas internas.

## Orquestación y diagnóstico

Arranque local completo (dev):

```bash
cd infra/compose/sgivu-docker-compose
./run.bash --dev
```

Diagnóstico rápido:

```bash
docker compose ps
docker compose logs -f sgivu-config
curl -s http://localhost:8080/actuator/health
```

## Diagramas

- Archivos `.puml` en `docs/diagrams/`. Mantén los diagramas actualizados cuando cambian componentes/puertos.
- Visualiza con VS Code + PlantUML o vía Docker:

```bash
docker run --rm -v "$(pwd)":/workspace ghcr.io/plantuml/plantuml -tsvg docs/diagrams/**/*.puml
```

## Guías por servicio

Cada repo/microservicio incluye `AGENTS.md` con pautas específicas. Revísa-las antes de cambios puntuales:

- [apps/backend/sgivu-auth/AGENTS.md](apps/backend/sgivu-auth/AGENTS.md)
- [apps/backend/sgivu-user/AGENTS.md](apps/backend/sgivu-user/AGENTS.md)
- [apps/backend/sgivu-client/AGENTS.md](apps/backend/sgivu-client/AGENTS.md)
- [apps/backend/sgivu-vehicle/AGENTS.md](apps/backend/sgivu-vehicle/AGENTS.md)
- [apps/backend/sgivu-purchase-sale/AGENTS.md](apps/backend/sgivu-purchase-sale/AGENTS.md)
- [apps/backend/sgivu-gateway/AGENTS.md](apps/backend/sgivu-gateway/AGENTS.md)
- [apps/backend/sgivu-discovery/AGENTS.md](apps/backend/sgivu-discovery/AGENTS.md)
- [apps/backend/sgivu-config/AGENTS.md](apps/backend/sgivu-config/AGENTS.md)
- [apps/ml/sgivu-ml/AGENTS.md](apps/ml/sgivu-ml/AGENTS.md)
- [apps/frontend/sgivu-frontend/AGENTS.md](apps/frontend/sgivu-frontend/AGENTS.md)
- [infra/compose/sgivu-docker-compose/AGENTS.md](infra/compose/sgivu-docker-compose/AGENTS.md)
- [https://github.com/stevenrq/sgivu-config-repo/blob/main/AGENTS.md](https://github.com/stevenrq/sgivu-config-repo/blob/main/AGENTS.md)

## ✅ Review checklist rápida

- Código claro, modular y probado.
- Comentarios/docstrings en español cuando agregan valor.
- README(s) y diagramas actualizados.
- Sin secretos ni credenciales.
- Logs/errores con contexto (no sensibles) y trazabilidad habilitada cuando aplique.
