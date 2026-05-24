"""Configuración de la aplicación sgivu-ml.

Centraliza todas las variables de entorno y valores por defecto usando
Pydantic BaseSettings V2.  El DSN de base de datos se genera con driver
``asyncpg`` para compatibilidad con SQLAlchemy async.
"""

import json
from functools import lru_cache
from pathlib import Path
from typing import Any
from urllib.parse import quote_plus

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic_settings.sources import EnvSettingsSource


class LenientEnvSettingsSource(EnvSettingsSource):
    """Fuente de configuración de entorno que maneja valores complejos.

    Permite leer valores complejos desde variables de entorno de forma
    leniente, evitando fallos por JSON inválido o bytes en bruto.
    """

    def decode_complex_value(self, field_name: str, field: Any, value: Any) -> Any:
        """Decodifica un valor complejo desde la fuente de entorno.

        Intenta delegar en la implementación base y, si se produce un
        error de JSON, devuelve el valor original sin lanzar excepción.
        """
        if value is None:
            return value
        if isinstance(value, (bytes, bytearray)):
            value = value.decode()
        if isinstance(value, str) and value.strip() == "":
            return value
        try:
            return super().decode_complex_value(field_name, field, value)
        except json.JSONDecodeError:
            return value


class Settings(BaseSettings):
    """Configuración de la aplicación sgivu-ml."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "sgivu-ml"
    app_version: str = "0.1.0"
    environment: str = "prod"

    sgivu_purchase_sale_url: str = "http://sgivu-purchase-sale"
    sgivu_vehicle_url: str = "http://sgivu-vehicle"

    sgivu_auth_discovery_url: str | None = None
    service_internal_secret_key: str | None = None

    database_url: str | None = None
    database_run_migrations: bool = True
    database_echo: bool = False
    database_env: str | None = None
    dev_ml_db_host: str | None = None
    dev_ml_db_port: int | None = 5432
    dev_ml_db_name: str | None = None
    dev_ml_db_username: str | None = None
    dev_ml_db_password: str | None = None
    prod_ml_db_host: str | None = None
    prod_ml_db_port: int | None = 5432
    prod_ml_db_name: str | None = None
    prod_ml_db_username: str | None = None
    prod_ml_db_password: str | None = None

    model_dir: str = "models"
    model_name: str = "demand_forecaster"
    default_horizon_months: int = 6
    request_timeout_seconds: float = 15.0
    retrain_cron: str = "0 3 1 * *"  # día 1 de cada mes a las 3 AM UTC
    retrain_timezone: str = "UTC"
    # Meses únicos requeridos en el dataset directo. Exige N+1 meses de
    # transacciones reales (ej.: 6 aquí → 7 meses de datos). Bajar a 1 en dev.
    min_history_months: int = 6
    target_column: str = "sales_count"

    # -- Residuales y cold-start -----------------------------------------------

    # Segmentos con menos meses que este valor reciben lags rellenos con la
    # media global (cold-start fallback).
    cold_start_min_months: int = 3
    # Shrinkage bayesiano de residuales por segmento:
    #   blended = (n·seg_std + prior·global_std) / (n + prior)
    # Prior alto → confía más en la std global; bajo → confía más en el segmento.
    residual_shrinkage_prior: int = 5

    # -- Calidad de datos ------------------------------------------------------

    # Multiplica el IQR para el clipping de outliers en ventas y compras.
    # 1.5 = agresivo, 3.0 = conservador (solo recorta valores extremos).
    outlier_iqr_multiplier: float = 3.0

    # -- Tuning de hiperparámetros ------------------------------------------------

    # Activa RandomizedSearchCV + TimeSeriesSplit. Desactivar en dev o con
    # datasets pequeños para reducir el tiempo de entrenamiento.
    enable_hyperparameter_tuning: bool = True
    # Folds de TimeSeriesSplit. Usar 2 con pocos datos para evitar folds vacíos.
    cv_folds: int = 3
    # Procesos paralelos en RandomizedSearchCV. -1 = todos los cores.
    cv_n_jobs: int = -1
    # Combinaciones aleatorias evaluadas por candidato. Más = mejor búsqueda,
    # más lento. Con datasets pequeños basta 5–10.
    cv_n_iter: int = 10
    # Métrica de selección del mejor modelo: "rmse", "mape" o "weighted".
    model_selection_metric: str = "weighted"
    # Peso del MAPE en la métrica "weighted"; el resto va al RMSE.
    # 0.0 = solo RMSE · 1.0 = solo MAPE.
    model_selection_mape_weight: float = 0.4

    # -- Predicción directa multi-step -----------------------------------------

    # Reducción del peso del modelo por paso:
    #   alpha = max(floor, 1 − rate·h) → predicción = alpha·modelo + (1−alpha)·baseline
    # Usar 0.0 para desactivar el dampening.
    forecast_dampening_rate: float = 0.05
    # Peso mínimo del modelo en el dampening. 0.5 = el modelo siempre aporta ≥50 %.
    forecast_dampening_floor: float = 0.5
    # Pasos de horizonte para los que se generan pares de entrenamiento. Aumentar
    # captura horizontes más largos a costa de más tiempo de entrenamiento.
    max_training_horizon: int = 12
    # Ventana corta para lag y rolling mean (lag_3, rolling_mean_3).
    lag_window_short: int = 3
    # Ventana larga para lag y rolling mean (lag_6, rolling_mean_6).
    # Usar 12 para capturar estacionalidad anual (requiere ≥13 meses de historia).
    lag_window_long: int = 6

    # -- Feature importance ----------------------------------------------------

    # Extrae y persiste la importancia de features tras cada entrenamiento.
    enable_feature_importance: bool = True

    permissions_predict: list[str] = ["ml:predict"]
    permissions_retrain: list[str] = ["ml:retrain"]
    permissions_models: list[str] = ["ml:models"]

    @field_validator(
        "permissions_predict",
        "permissions_retrain",
        "permissions_models",
        mode="before",
    )
    @classmethod
    def _split_permissions(cls, value: Any) -> Any:
        if isinstance(value, str):
            return [
                scope.strip()
                for scope in value.replace(" ", "").split(",")
                if scope.strip()
            ]
        return value

    def model_path(self) -> Path:
        """Devuelve la ruta al directorio de modelos.

        Crea el directorio si no existe y retorna la ruta como `Path`.
        """
        path = Path(self.model_dir)
        path.mkdir(parents=True, exist_ok=True)
        return path

    def _database_env(self) -> str:
        raw = (self.database_env or self.environment or "").strip().lower()
        if raw in ("prod", "production", "prd"):
            return "prod"
        if raw in ("dev", "development", "local"):
            return "dev"
        return "dev"

    def _database_fields(
        self,
    ) -> tuple[str | None, int | None, str | None, str | None, str | None]:
        if self._database_env() == "prod":
            return (
                self.prod_ml_db_host,
                self.prod_ml_db_port,
                self.prod_ml_db_name,
                self.prod_ml_db_username,
                self.prod_ml_db_password,
            )
        return (
            self.dev_ml_db_host,
            self.dev_ml_db_port,
            self.dev_ml_db_name,
            self.dev_ml_db_username,
            self.dev_ml_db_password,
        )

    def _build_dsn(self, driver: str) -> str | None:
        """Genera el DSN de conexión con el driver indicado."""
        if self.database_url:
            dsn = self.database_url
            for old_prefix in (
                "postgresql+psycopg2://",
                "postgresql+asyncpg://",
                "postgresql+psycopg://",
                "postgresql://",
            ):
                if dsn.startswith(old_prefix):
                    dsn = dsn.replace(old_prefix, f"postgresql+{driver}://", 1)
                    break
            return dsn

        host, port, name, user, password = self._database_fields()
        if not host or not name or not user:
            return None
        port = port or 5432
        user = quote_plus(user)
        password = password or ""
        password_enc = quote_plus(password)
        auth = f"{user}:{password_enc}" if password else user
        return f"postgresql+{driver}://{auth}@{host}:{port}/{name}"

    def database_dsn(self) -> str | None:
        """DSN async (``asyncpg``) para el runtime de la aplicación."""
        return self._build_dsn("asyncpg")

    def database_sync_dsn(self) -> str | None:
        """DSN sync (``psycopg``) para Alembic y operaciones síncronas."""
        return self._build_dsn("psycopg")

    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls: type,
        init_settings: Any,
        env_settings: Any,
        dotenv_settings: Any,
        file_secret_settings: Any,
    ) -> tuple:
        """Personaliza las fuentes de Settings para soporte leniente de env.

        Inserta `LenientEnvSettingsSource` como la fuente de entorno para
        manejar variables de entorno complejas sin fallos de parseo.
        """
        return (
            init_settings,
            LenientEnvSettingsSource(settings_cls),
            dotenv_settings,
            file_secret_settings,
        )


@lru_cache
def get_settings() -> Settings:
    """Obtiene una instancia cacheada de `Settings`.

    Esta función utiliza LRU cache para retornar siempre la misma instancia
    durante el ciclo de vida de la aplicación. Útil para inyectar configuración
    sin recrear objetos repetidamente.

    Returns
    -------
    Settings
        Instancia configurada de la aplicación.
    """
    return Settings()
