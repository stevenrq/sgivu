# Guía del Repositorio

## Estructura del Proyecto y Módulos

- `app/`: servicio FastAPI; `main.py` arranque, `core/` configuración y seguridad, `routers/prediction_router.py` rutas, `services/` carga de datos, entrenamiento y predicción, `models/` esquemas Pydantic, `dependencies.py` para DI.
- `models/`: artefactos joblib y `latest.json` generados por entrenamiento; montar como volumen en despliegues. `scripts/cron_retrain.sh` programa reentrenos; `run.sh`, `Dockerfile` y `build-image.bash` cubren arranque y build.
- `tests/`: demo offline (`csv_offline_demo.py`, `run_offline_demo_db.sh`, `run_offline_demo_memory.sh`) y datos sintéticos en `tests/data/`; `tests/models_offline/` está ignorado. `../../../docs/diagrams/` guarda diagramas PlantUML.

## Comandos de Build, Pruebas y Desarrollo

- Preparar entorno (Python 3.12): `python -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt`.
- API local con recarga: `UVICORN_CMD="uvicorn app.main:app --reload --host 0.0.0.0 --port 8000" ./run.sh`.
- Smoke test sin dependencias HTTP: `tests/run_offline_demo_memory.sh` o `tests/run_offline_demo_db.sh` (genera `tests/data/forecast.png` y artefactos en `tests/models_offline/`).
- Imagen Docker: `./build-image.bash`; para uso local sin push `docker build -t sgivu-ml:dev .` y montar `models/` al ejecutar.

## Estilo de Código y Convenciones

- Código en inglés; docstrings y comentarios en español. Identación de 4 espacios y PEP8.
- Nombres en `snake_case`, tipado completo; usar `Field` en modelos Pydantic para validaciones breves.
- Rutas delgadas; la lógica vive en `app/services/*`. No versionar artefactos de modelos ni credenciales; copiar `.env` desde `.env.example`.

## Guía de Pruebas

- Aún sin suite pytest; usar `tests/run_offline_demo.sh` o `tests/csv_offline_demo.py --csv ...` como verificación básica.
- Para nuevas pruebas, ubicarlas en `tests/test_*.py`; mockear llamadas a gateway/auth y evitar servicios reales.
- Tras reentrenar, validar que `models/latest.json` y las respuestas sigan compatibles.

## Commits y Pull Requests

- Commits en inglés con Conventional Commits (`feat: ...`, `fix: ...`), pequeños y enfocados.
- PRs deben incluir propósito, issue relacionado, comandos de prueba ejecutados y cambios de env/volúmenes. Adjuntar request/response o capturas si cambia el contrato o la demo offline.

## Seguridad y Configuración

- La validación JWT depende de `SGIVU_AUTH_DISCOVERY_URL` o `AUTH_*`; no subir tokens ni datasets reales. Para llamadas internas usar `SERVICE_INTERNAL_SECRET_KEY` solo en variables de entorno.
- Asegurar que `MODEL_DIR` exista y se monte en Docker para persistir artefactos. Configurar `REQUEST_TIMEOUT_SECONDS` acorde a latencia del gateway.

## Notas Específicas del Servicio

- models/ no se versiona en producción; monta el volumen en despliegues y documenta la versión del modelo en `latest.json`.
