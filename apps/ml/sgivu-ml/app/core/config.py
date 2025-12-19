import json
from functools import lru_cache
from pathlib import Path
from typing import Any
from urllib.parse import quote_plus

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic_settings.sources import EnvSettingsSource


class LenientEnvSettingsSource(EnvSettingsSource):
    """Evita fallar en listas vacías o valores no JSON para scopes."""

    def decode_complex_value(self, field_name: str, field: Any, value: Any) -> Any:
        """Interpreta valores compuestos tolerando strings vacíos o inválidos."""
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
    """Parámetros globales del microservicio de predicción SGIVU-ML.

    Incluye endpoints de origen de datos (gateway), configuración de seguridad
    JWT/JWKS, ubicación de artefactos de modelo y ajustes de ventana temporal
    para el pipeline de demanda.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "sgivu-ml"
    app_version: str = "0.1.0"
    environment: str = "dev"

    sgivu_purchase_sale_url: str = "http://sgivu-vehicle"
    sgivu_vehicle_url: str = "http://sgivu-purchase-sale"

    sgivu_auth_discovery_url: str | None = None
    service_internal_secret_key: str | None = None

    database_url: str | None = None
    database_auto_create: bool = False
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
    retrain_cron: str = "0 3 1 * *"
    retrain_timezone: str = "UTC"
    min_history_months: int = 6
    target_column: str = "sales_count"
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
    def _split_permissions(cls, value):
        """Normaliza listas de scopes leídas desde variables de entorno."""
        if isinstance(value, str):
            return [
                scope.strip()
                for scope in value.replace(" ", "").split(",")
                if scope.strip()
            ]
        return value

    def model_path(self) -> Path:
        """Crea (si no existe) y devuelve la ruta al directorio de modelos."""
        path = Path(self.model_dir)
        path.mkdir(parents=True, exist_ok=True)
        return path

    def _database_env(self) -> str:
        """Determina si se usa la DB de dev o prod segun DATABASE_ENV/ENVIRONMENT."""
        raw = (self.database_env or self.environment or "").strip().lower()
        if raw in ("prod", "production", "prd"):
            return "prod"
        if raw in ("dev", "development", "local"):
            return "dev"
        return "dev"

    def _database_fields(
        self,
    ) -> tuple[str | None, int | None, str | None, str | None, str | None]:
        """Selecciona las variables segun el entorno activo."""
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

    def database_dsn(self) -> str | None:
        """Construye la URL de base de datos usando DATABASE_URL o *_ML_DB_*."""
        if self.database_url:
            return self.database_url
        host, port, name, user, password = self._database_fields()
        if not host or not name or not user:
            return None
        port = port or 5432
        user = quote_plus(user)
        password = password or ""
        password_enc = quote_plus(password)
        auth = f"{user}:{password_enc}" if password else user
        return f"postgresql+psycopg2://{auth}@{host}:{port}/{name}"

    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls,
        init_settings,
        env_settings,
        dotenv_settings,
        file_secret_settings,
    ):
        """Inserta la fuente leniente antes de dotenv y secretos en disco."""
        return (
            init_settings,
            LenientEnvSettingsSource(settings_cls),
            dotenv_settings,
            file_secret_settings,
        )


@lru_cache
def get_settings() -> Settings:
    """Devuelve la configuración cacheada (singleton)."""
    return Settings()
