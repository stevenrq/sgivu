# sgivu-ml - SGIVU

## Descripción

**`sgivu-ml`** es el microservicio de modelado y predicción del ecosistema **SGIVU**. Expone APIs REST para realizar predicciones (por ejemplo: demanda estimada por segmento), consultar metadata del modelo y disparar reentrenamientos. Está implementado con FastAPI y se ejecuta con Uvicorn; los artefactos de modelo se versionan con `joblib` y pueden persistirse opcionalmente en PostgreSQL.

## Tecnologías y Dependencias

- Python 3.12
- FastAPI + Uvicorn
- scikit-learn, XGBoost (opcional), joblib
- pandas, numpy
- SQLAlchemy + psycopg2 (PostgreSQL)
- Authlib / PyJWT (validación de JWT)
- Pydantic (configuración y validación)

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
./build-image.bash
docker run --env-file infra/compose/sgivu-docker-compose/.env -p 8000:8000 stevenrq/sgivu-ml:v1
```

### Producción

En producción el servicio normalmente se expone a través de `sgivu-gateway` que enruta `/v1/ml/**` a `http://sgivu-ml:8000`.

## Puertos

- Puerto por defecto: **8000** (Uvicorn)

## Endpoints principales

| Endpoint | Método | Descripción |
| --- | ---: | --- |
| `/actuator/health` ó `/health` | GET | Health check básico |
| `/v1/ml/predict` | POST | Solicitar predicción (payload: features) |
| `/v1/ml/predict-with-history` | POST | Predicción + historial para graficar |
| `/v1/ml/retrain` | POST | Disparar reentrenamiento (opcional con rango de fechas) |
| `/v1/ml/models/latest` | GET | Metadata del último modelo entrenado |
| `/docs` | GET | Documentación OpenAPI (UI de FastAPI) |

> Consultar `app/main.py` y los routers en `app/routers/` para la definición exacta de rutas y payloads.

## Seguridad

- Validación de tokens JWT mediante OIDC (configurable, `SGIVU_AUTH_DISCOVERY_URL` / parámetros relacionados).
- Para llamadas internas entre servicios se soporta el encabezado `X-Internal-Service-Key` (clave compartida interna). **No exponer** esta clave públicamente.
- Recomendación: el `sgivu-gateway` debe manejar el token y propagarlo hacia `sgivu-ml` si corresponde.

## Base de datos

- Si se habilita persistencia en PostgreSQL, el esquema y scripts de creación se encuentran en `apps/ml/sgivu-ml/app/database/schema.sql`. El servicio puede persistir:
  - Artefactos de modelo (`ml_model_artifacts`)
  - Snapshots de features (`ml_training_features`)
  - Registros de predicción (`ml_predictions`)

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
