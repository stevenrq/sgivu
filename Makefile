SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

COMPOSE_DIR   := infra/compose/sgivu-docker-compose
SCRIPTS_DIR   := $(COMPOSE_DIR)/scripts
COMPOSE_DEV   := docker compose -f docker-compose.dev.yml --env-file .env.dev
ML_DIR        := apps/ml/sgivu-ml

JAVA_SERVICES := sgivu-auth sgivu-client sgivu-config sgivu-discovery \
                 sgivu-gateway sgivu-user sgivu-vehicle sgivu-purchase-sale
PY_SERVICES   := sgivu-ml
ALL_SERVICES  := $(JAVA_SERVICES) $(PY_SERVICES)

SERVICE ?=
DB      ?= user

# servicio -> directorio
dir_of = $(if $(filter $(1),$(PY_SERVICES)),apps/ml/$(1),apps/backend/$(1))

define require_service
	@if [ -z "$(SERVICE)" ]; then echo "Uso: make $@ SERVICE=<servicio>"; exit 1; fi
endef

##@ General

.PHONY: help
help: ## Muestra esta ayuda
	@awk 'BEGIN{FS=":.*##"; printf "\nUso: make \033[36m<target>\033[0m [SERVICE=sgivu-user]\n"} \
	/^##@/{printf "\n\033[1m%s\033[0m\n", substr($$0,5)} \
	/^[a-zA-Z_-]+:.*?##/{printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.PHONY: setup
setup: ## Onboarding único: instala uv/pre-commit, hooks, venv ML y .env.dev
	@command -v uv >/dev/null || { echo "Instalando uv..."; curl -LsSf https://astral.sh/uv/install.sh | sh; }
	@export PATH="$$HOME/.local/bin:$$PATH"; \
	command -v pre-commit >/dev/null || uv tool install pre-commit; \
	pre-commit install
	@[ -f $(COMPOSE_DIR)/.env.dev ] || { cp $(COMPOSE_DIR)/.env.dev.example $(COMPOSE_DIR)/.env.dev; \
	  echo ">> Creado $(COMPOSE_DIR)/.env.dev — revisa y completa los valores"; }
	@cd $(ML_DIR) && [ -d .venv ] || ~/.local/bin/uv venv >/dev/null; \
	  cd $(CURDIR)/$(ML_DIR) && ~/.local/bin/uv pip install -q -r requirements.txt -r requirements-dev.txt
	@for tool in docker java make gh; do \
	  command -v $$tool >/dev/null || echo ">> FALTA: $$tool (instálalo: sudo apt install $$tool)"; done
	@echo "Setup completo. Prueba: make dev"

##@ Desarrollo (infra en Docker + apps en host con hot-reload)

.PHONY: dev
dev: ## Levanta todo: infra Docker + apps en host (hot-reload)
	cd $(COMPOSE_DIR) && ./scripts/start-dev.sh

.PHONY: dev-status
dev-status: ## Estado de los servicios corriendo en host
	cd $(COMPOSE_DIR) && ./scripts/start-dev.sh --status

.PHONY: dev-stop
dev-stop: ## Detiene las apps en host (infra Docker sigue)
	cd $(COMPOSE_DIR) && ./scripts/start-dev.sh --stop

.PHONY: dev-down
dev-down: ## Detiene apps host Y apaga la infra Docker
	cd $(COMPOSE_DIR) && ./scripts/start-dev.sh --all-down

.PHONY: infra-up
infra-up: ## Solo infra (Postgres, Redis, Config, Discovery)
	cd $(COMPOSE_DIR) && ./scripts/dev-up.sh

.PHONY: infra-stop
infra-stop: ## Para la infra sin eliminar contenedores (preserva sesiones)
	cd $(COMPOSE_DIR) && ./scripts/dev-up.sh --stop

.PHONY: infra-down
infra-down: ## Apaga y elimina los contenedores de infra
	cd $(COMPOSE_DIR) && ./scripts/dev-up.sh --down

.PHONY: host-run
host-run: ## Corre un servicio en host: make host-run SERVICE=sgivu-user
	$(require_service)
	cd $(COMPOSE_DIR) && ./scripts/host-run.sh $(SERVICE)

.PHONY: host-logs
host-logs: ## Logs de un servicio en host: make host-logs SERVICE=sgivu-user
	$(require_service)
	cd $(COMPOSE_DIR) && ./scripts/host-run.sh --logs $(SERVICE)

##@ Stack completo en Docker (dev)

.PHONY: up
up: ## Levanta el stack completo dockerizado (docker-compose.dev.yml)
	cd $(COMPOSE_DIR) && ./scripts/run.sh --dev

.PHONY: down
down: ## Apaga el stack dockerizado
	cd $(COMPOSE_DIR) && $(COMPOSE_DEV) down

.PHONY: restart
restart: ## Reinicia contenedor(es): make restart [SERVICE=sgivu-user]
	cd $(COMPOSE_DIR) && $(COMPOSE_DEV) restart $(SERVICE)

.PHONY: rebuild
rebuild: ## Reconstruye imagen y recrea contenedor: make rebuild SERVICE=sgivu-user
	$(require_service)
	cd $(COMPOSE_DIR) && ./scripts/rebuild-service.sh --dev $(SERVICE)

.PHONY: ps
ps: ## Estado de los contenedores
	cd $(COMPOSE_DIR) && $(COMPOSE_DEV) ps

.PHONY: logs
logs: ## Logs de contenedores: make logs [SERVICE=sgivu-user]
	cd $(COMPOSE_DIR) && $(COMPOSE_DEV) logs -f $(SERVICE)

##@ Build, tests y calidad

.PHONY: build
build: ## Compila jar(s): make build [SERVICE=sgivu-user]
ifeq ($(SERVICE),)
	@for s in $(JAVA_SERVICES); do \
	  echo ">> build $$s"; (cd apps/backend/$$s && ./mvnw -B -q package -DskipTests) || exit 1; done
else ifneq ($(filter $(SERVICE),$(JAVA_SERVICES)),)
	cd $(call dir_of,$(SERVICE)) && ./mvnw -B package -DskipTests
else
	@echo "$(SERVICE) no requiere build (Python)"
endif

.PHONY: test
test: ## Ejecuta tests: make test [SERVICE=sgivu-user]
ifeq ($(SERVICE),)
	@for s in $(JAVA_SERVICES); do \
	  echo ">> test $$s"; (cd apps/backend/$$s && ./mvnw -B -q test) || exit 1; done
	@echo ">> test sgivu-ml"; cd $(ML_DIR) && .venv/bin/pytest -q tests
else ifneq ($(filter $(SERVICE),$(JAVA_SERVICES)),)
	cd $(call dir_of,$(SERVICE)) && ./mvnw -B test
else
	cd $(ML_DIR) && .venv/bin/pytest -q tests
endif

.PHONY: lint
lint: ## Verifica formato/lint sin modificar: make lint [SERVICE=...]
ifeq ($(SERVICE),)
	@for s in $(JAVA_SERVICES); do \
	  echo ">> lint $$s"; (cd apps/backend/$$s && ./mvnw -B -q spotless:check) || exit 1; done
	@echo ">> lint sgivu-ml"; cd $(ML_DIR) && .venv/bin/black --check . && .venv/bin/pylint app/ tests/
else ifneq ($(filter $(SERVICE),$(JAVA_SERVICES)),)
	cd $(call dir_of,$(SERVICE)) && ./mvnw -B spotless:check
else
	cd $(ML_DIR) && .venv/bin/black --check . && .venv/bin/pylint app/ tests/
endif

.PHONY: format
format: ## Aplica formato/lint-fix: make format [SERVICE=...]
ifeq ($(SERVICE),)
	@for s in $(JAVA_SERVICES); do \
	  echo ">> format $$s"; (cd apps/backend/$$s && ./mvnw -B -q spotless:apply) || exit 1; done
	@echo ">> format sgivu-ml"; cd $(ML_DIR) && .venv/bin/black .
else ifneq ($(filter $(SERVICE),$(JAVA_SERVICES)),)
	cd $(call dir_of,$(SERVICE)) && ./mvnw -B spotless:apply
else
	cd $(ML_DIR) && .venv/bin/black .
endif

.PHONY: precommit
precommit: ## Corre todos los hooks de pre-commit sobre el repo
	@export PATH="$$HOME/.local/bin:$$PATH" && pre-commit run --all-files

##@ Imágenes Docker

.PHONY: image
image: ## Construye imagen local: make image SERVICE=sgivu-user
	$(require_service)
	docker build -t stevenrq/$(SERVICE):latest $(call dir_of,$(SERVICE))

.PHONY: images
images: ## Construye todas las imágenes locales
	@for s in $(ALL_SERVICES); do \
	  echo ">> image $$s"; $(MAKE) --no-print-directory image SERVICE=$$s || exit 1; done

##@ Base de datos

.PHONY: db-shell
db-shell: ## psql a una BD: make db-shell DB=user|auth|client|vehicle|purchase_sale|ml
	docker exec -it sgivu-postgres psql -U postgres -d sgivu_$(DB)_db

.PHONY: db-backup
db-backup: ## Backup de Postgres y Redis (dev)
	cd $(COMPOSE_DIR) && ./scripts/dbs-backups.sh --dev

.PHONY: alembic-upgrade
alembic-upgrade: ## Aplica migraciones de sgivu-ml (requiere DB env vars)
	cd $(ML_DIR) && .venv/bin/alembic upgrade head

.PHONY: alembic-rev
alembic-rev: ## Nueva migración: make alembic-rev MSG="add table"
	@if [ -z "$(MSG)" ]; then echo "Uso: make alembic-rev MSG=\"mensaje\""; exit 1; fi
	cd $(ML_DIR) && .venv/bin/alembic revision --autogenerate -m "$(MSG)"

##@ CI/CD

.PHONY: deploy
deploy: ## Despliega a EC2: make deploy [SERVICES=sgivu-user,sgivu-ml] [TAG=abc1234]
	gh workflow run deploy.yml --ref main -f services=$(or $(SERVICES),all) $(if $(TAG),-f tag=$(TAG))
	@echo "Disparado. Sigue el progreso con: make deploy-watch"

.PHONY: deploy-watch
deploy-watch: ## Sigue en vivo el último run del deploy
	gh run watch $$(gh run list --workflow=deploy.yml -L1 --json databaseId -q '.[0].databaseId')

.PHONY: ci-status
ci-status: ## Últimos runs de CI
	gh run list --workflow=ci.yml -L5

##@ Limpieza

.PHONY: clean
clean: ## mvn clean en todos los servicios + borra logs de host-run
	@for s in $(JAVA_SERVICES); do (cd apps/backend/$$s && ./mvnw -q clean); done
	@rm -f $(COMPOSE_DIR)/.host-run/*.log 2>/dev/null || true
	@echo "Limpio."

.PHONY: clean-docker
clean-docker: ## PELIGRO: elimina TODOS los contenedores e imágenes de la máquina
	@read -p "Esto borra TODOS los contenedores/imágenes de Docker (no solo sgivu). ¿Continuar? [y/N] " r; \
	  [ "$$r" = "y" ] && (cd $(COMPOSE_DIR) && ./scripts/remove-containers-images.sh) || echo "Cancelado."
