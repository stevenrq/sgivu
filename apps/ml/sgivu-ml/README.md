# sgivu-ml - SGIVU

## Descripción

**`sgivu-ml`** es el microservicio de modelado y predicción del ecosistema **SGIVU**. Expone APIs REST para realizar predicciones (por ejemplo: demanda estimada por segmento), consultar metadata del modelo y disparar reentrenamientos. Está implementado con FastAPI y se ejecuta con Uvicorn; los artefactos de modelo se versionan con `joblib` y pueden persistirse opcionalmente en PostgreSQL.

## Tecnologías y Dependencias

- Python 3.12
- FastAPI + Uvicorn
- scikit-learn, XGBoost, joblib
- pandas, numpy, scipy, matplotlib
- SQLAlchemy 2.0 (asyncio) + asyncpg (runtime) y psycopg (Alembic)
- Alembic (migraciones de esquema ML)
- Authlib + cryptography (validación JWT vía OIDC)
- Pydantic v2 + pydantic-settings (configuración y validación)
- httpx (cliente HTTP a `sgivu-purchase-sale` y `sgivu-vehicle`)

(Revisar `requirements.txt` para la lista completa de paquetes.)

## Requisitos Previos

- Python 3.12 (o usar la imagen Docker proporcionada)
- PostgreSQL si desea persistir modelos, snapshots o registros de predicción
- `sgivu-config` / `sgivu-auth` operativos si se valida JWT vía OIDC

## Arranque y Ejecución

### Desarrollo (local)

1. Crear y exportar variables de entorno necesarias (ver sección "Variables de entorno" más abajo).
2. Instalar dependencias:

   ```bash
   python -m pip install -r requirements.txt
   ```

3. Ejecutar servicio en modo dev:

   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

### Demo offline

- Existen scripts de demo y entrenamiento en `tests/` para ejecutar pipelines de entrenamiento y generar predicciones de ejemplo sin depender de la DB.

### Docker

```bash
./build-image.sh          # construye localmente
./build-image.sh --push   # construye y publica en Docker Hub

docker run --env-file infra/compose/sgivu-docker-compose/.env -p 8000:8000 stevenrq/sgivu-ml:0.1.0
```

### Producción

En producción el servicio normalmente se expone a través de `sgivu-gateway` que enruta `/v1/ml/**` a `http://sgivu-ml:8000`.

## Puertos

- Puerto por defecto: **8000** (Uvicorn)

## Endpoints principales

| Endpoint | Método | Permiso | Descripción |
| --- | ---: | --- | --- |
| `/health` | GET | Ninguno (público) | Health check básico (`{"status": "ok", "version": "0.1.0"}`) |
| `/v1/ml/predict` | POST | `ml:predict` | Pronóstico de demanda por segmento (1–24 meses) |
| `/v1/ml/predict-with-history` | POST | `ml:predict` | Predicción + historial para graficar |
| `/v1/ml/retrain` | POST | `ml:retrain` | Reentrenamiento del modelo (opcionalmente con rango de fechas). El gateway reserva 1800 s para esta ruta. |
| `/v1/ml/models/latest` | GET | `ml:models` | Metadata del último modelo entrenado |
| `/v1/ml/models/latest/feature-importance` | GET | `ml:models` | Importancias de features del modelo activo |
| `/v1/ml/actuals` | POST | `ml:retrain` | Registrar demanda real para drift |
| `/v1/ml/drift-report` | GET | `ml:models` | Reporte de drift por versión de modelo |
| `/docs` | GET | Ninguno | OpenAPI UI (FastAPI) |

> Consultar `app/api/routers/{health,prediction,monitoring}.py` para la definición exacta. Las llamadas internas con `X-Internal-Service-Key` omiten la verificación de permisos.

### Algoritmos y selección

- Candidatos: **Random Forest** y **XGBoost** (no LinearRegression).
- Búsqueda de hiperparámetros: `RandomizedSearchCV` (`cv_n_iter=10`, `cv_n_jobs=-1`) con `TimeSeriesSplit(n_splits=3)`.
- Selección por defecto: score compuesto `(1 - mape_weight) * RMSE + mape_weight * WAPE` con `mape_weight = 0.4`. Configurable vía `MODEL_SELECTION_METRIC` (`weighted` | `rmse` | `mape`).
- Forecasting **multi-step directo** con dampening (`forecast_dampening_rate=0.05`, floor `0.5`); calibración de residuos por horizonte para los intervalos de confianza.
- Versionado de modelo: timestamp UTC en formato `YYYYMMDDHHMMSS`. Persistencia en filesystem (`{MODEL_NAME}_{version}.joblib` + `latest.json`) o en PostgreSQL (`ml_model_artifacts.artifact BYTEA`).
- Cron interno de reentrenamiento: `RETRAIN_CRON=0 3 1 * *` (día 1 de cada mes, 3 AM UTC).

## Seguridad

- Validación de tokens JWT mediante OIDC (configurable, `SGIVU_AUTH_DISCOVERY_URL` / parámetros relacionados).
- Para llamadas internas entre servicios se soporta el encabezado `X-Internal-Service-Key` (clave compartida interna). **No exponer** esta clave públicamente.
- Recomendación: el `sgivu-gateway` debe manejar el token y propagarlo hacia `sgivu-ml` si corresponde.

## Base de datos

- El esquema se gestiona con **Alembic** (`alembic/versions/`). Las migraciones se ejecutan al arranque si `DATABASE_RUN_MIGRATIONS=true` (por defecto).
- Conexión: `postgresql+asyncpg://...`. Selecciona credenciales `DEV_ML_DB_*` o `PROD_ML_DB_*` según `ENVIRONMENT`.
- Tablas mantenidas:
  - `ml_model_artifacts` — pipelines serializados (BYTEA) + metadatos JSON
  - `ml_training_features` — snapshots de features por segmento/mes
  - `ml_drift_records` — registros predicción vs real para monitoreo de drift
  - `ml_predictions` — auditoría de requests/responses de predicciones

## Observabilidad

- OpenAPI docs disponibles en `/docs` (FastAPI).

## Pruebas

- Hay scripts de demo y tests de integración/discovery en `tests/` para ejecución local y pruebas de entrenamiento/predicción.
- Recomendación: añadir tests unitarios e integración que validen el comportamiento de training/prediction y validaciones de payload.

## Solución de Problemas

| Problema | Posible causa | Acción |
| --- | --- | --- |
| Sin modelo disponible | No se ha entrenado o no hay artefactos en `MODEL_DIR` ni DB | Ejecutar pipeline de entrenamiento o cargar artefacto de modelo |
| Error de conexión a BD | `DATABASE_URL` mal o DB inaccesible | Revisar variables de entorno y conectividad a PostgreSQL |
| 401/403 en predicción | Token JWT inválido o falta `X-Internal-Service-Key` | Verificar issuer, scopes y header interno |
| Predicciones inconsistentes | Data/features fuera de distribución esperada | Revisar preprocesamiento y reglas de features; reentrenar si es necesario |

## Contribuciones

1. Fork → branch → PR
2. Incluir tests para cambios funcionales (especialmente training/prediction)
