# SGIVU - sgivu-ml

## Descripción

Servicio FastAPI para estimar demanda mensual de vehículos usados por tipo/marca/línea/modelo, integrado con los microservicios SGIVU.

## Arquitectura y Rol

- Servicio Python (FastAPI) que consume datos vía `sgivu-gateway` respetando OAuth2 y circuit breakers.
- Usa contratos y datos de inventario para preparar dataset y generar predicciones de demanda.
- Persiste modelos, snapshots de entrenamiento y predicciones en PostgreSQL cuando `DATABASE_URL` está configurada; fallback a disco en `models/` si no hay DB.

### Fuentes de datos

- Gateway hacia `sgivu-purchase-sale` (`/v1/purchase-sales/search`, `/v1/purchase-sales/detailed`).
- Gateway hacia inventario (`/v1/cars/{id}`, `/v1/motorcycles/{id}`) para atributos de vehículo.
- Campos clave: contractType/status, precios, paymentMethod, fechas, vehicleId, clientId, userId, VehicleSummary.

### Diseño del dataset

- Objetivo: `sales_count` mensual por segmento (vehicle_type, brand, line, model).
- Features: precios compra/venta, margen, días en inventario, rotación, estacionalidad (mes/año, seno/coseno), lags/rolling (1/3/6), estado de contrato y vehículo.

### Pipeline de entrenamiento

1) Carga async de contratos vía gateway + enriquecimiento de inventario.
2) Normalización de categorías/fechas y cálculo de margen/días en inventario.
3) Agregación mensual por segmento.
4) Features temporales (lags/rolling/estacionalidad/rotación).
5) Split temporal 80/20 y evaluación de modelos (LinearRegression, RandomForestRegressor, XGBRegressor si disponible).
6) Selección por RMSE y serialización en PostgreSQL (si `DATABASE_URL`) o en `models/{model_name}_{version}.joblib` + `models/latest.json`.
7) `/v1/ml/retrain` dispara entrenamiento; `scripts/cron_retrain.sh` permite cron.

## Tecnologías

- Lenguaje: Python 3.12
- Framework: FastAPI
- ML: scikit-learn, joblib (opcional XGBoost)
- Infraestructura: Docker, scripts Bash para cron/retrain

## Configuración

- Variables clave: `DATABASE_URL`, `DATABASE_ENV`, `SGIVU_PURCHASE_SALE_URL`, `SGIVU_VEHICLE_URL`, `SGIVU_AUTH_DISCOVERY_URL`, `AUTH_JWKS_URL`, `AUTH_PUBLIC_KEY`, `SERVICE_INTERNAL_SECRET_KEY`.
- `DATABASE_URL` tiene prioridad si se define; caso contrario usa `DEV_ML_DB_*` / `PROD_ML_DB_*`.
- `DATABASE_AUTO_CREATE=true` crea tablas en arranque (solo dev) o ejecuta `scripts/schema.sql`.

### Persistencia en PostgreSQL

- Configura `DATABASE_URL` (por ejemplo `postgresql://user:pass@host:5432/sgivu_ml`) o usa `DEV_ML_DB_*` / `PROD_ML_DB_*` para construirla.
- Selecciona el entorno con `DATABASE_ENV=dev|prod` (o usa `ENVIRONMENT`; `DATABASE_URL` tiene prioridad).
- Se almacenan: modelos serializados, snapshots de features mensuales y solicitudes de predicción.

## Ejecución Local

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
UVICORN_CMD="uvicorn app.main:app --reload --host 0.0.0.0 --port 8000" ./run.sh
```

### Prueba offline con CSV

- `tests/csv_offline_demo.py` permite entrenar y predecir sin microservicios.

```text
python tests/csv_offline_demo.py --csv /ruta/datos.csv --horizon 6 --vehicle-type MOTORCYCLE --brand YAMAHA --model "MT-03" --line ABC-89F
```

- Scripts rápidos:
  - `tests/run_offline_demo_db.sh`: usa PostgreSQL si está configurado.
  - `tests/run_offline_demo_memory.sh`: fuerza ejecución en memoria.
- Datos sintéticos: `tests/generate_contracts.py`; los scripts generan `tests/data/forecast.png` y artefactos en `tests/models_offline/`.

## Endpoints Principales

```text
GET  /health
POST /v1/ml/predict
POST /v1/ml/retrain
GET  /v1/ml/models/latest
```

- `/v1/ml/predict`: cuerpo con `vehicle_type`, `brand`, `model`, `line`, `horizon_months` (1-24), `confidence`.
- `/v1/ml/retrain`: opcional `start_date`, `end_date`; retorna versión y métricas.
- `/v1/ml/models/latest`: metadata del modelo activo.

## Seguridad

- Todas las rutas excepto `/health` requieren `Authorization: Bearer <JWT>`.
- Valida JWT OIDC con JWKS (`AUTH_JWKS_URL` o `SGIVU_AUTH_DISCOVERY_URL`) o llave pública (`AUTH_PUBLIC_KEY`); verifica `exp`, `iss`, `aud` si se configuran `AUTH_ISSUER`/`AUTH_AUDIENCE`.
- Permisos por claim `rolesAndPermissions`: `ml:predict`, `ml:retrain`, `ml:models` (ajustables vía `PERMISSIONS_*`).
- Clave interna opcional con `SERVICE_INTERNAL_SECRET_KEY` para llamadas service-to-service.

## Dependencias

- `sgivu-gateway` (fuente de datos), `sgivu-auth` (JWT), PostgreSQL para persistencia opcional, storage de artefactos en volumen `models/`.

## Dockerización

- Dockerfile basado en Python 3.12 slim; `run.sh` como entrypoint.
- En Compose dev: servicio `sgivu-ml` en puerto 8000 con volumen `sgivu-ml-models`.

## Build y Push Docker

- `./build-image.bash` limpia contenedores previos y publica `stevenrq/sgivu-ml:v1` (sin Maven al ser Python).

## Despliegue

- Montar volumen `models/` para persistir artefactos.
- Configurar URLs a gateway (`SGIVU_PURCHASE_SALE_URL`, `SGIVU_VEHICLE_URL`) y validación OIDC (`SGIVU_AUTH_DISCOVERY_URL` o `AUTH_*`).

## Monitoreo

- `/health` para checks; logs estructurados. Agregar Prometheus/OTel según necesidad.

## Troubleshooting

- 401/invalid_token: revisa JWKS/issuer/`PERMISSIONS_*`.
- Timeouts cargando datos: valida URLs de gateway y latencias.
- Modelos no se guardan: confirma `DATABASE_URL` o volumen `models/` y permisos de escritura.

## Buenas Prácticas y Convenciones

- Código en inglés; docs en español; commits en inglés con Conventional Commits.

## Diagramas

- Arquitectura general: `../../../docs/diagrams/01-system-architecture.puml`.

## Autor

- Steven Ricardo Quiñones (2025)
