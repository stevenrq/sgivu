"""Construcción del grafo de dependencias para los scripts de dev.

Replica la cadena de inyección de dependencias de
``app/api/dependencies.py`` fuera del contexto de FastAPI, permitiendo
que los scripts de desarrollo ejerciten exactamente el mismo código que
usa producción.

IMPORTANTE: estos scripts están diseñados exclusivamente para el entorno
de desarrollo. El bootstrap verifica que la configuración cargada
corresponda a dev y aborta si detecta configuración de producción.
Siempre deben ejecutarse a través de ``run_dev.sh``.
"""

from __future__ import annotations

import logging
import sys
from typing import Callable

from app.application.services.prediction_service import PredictionService
from app.application.services.training_service import TrainingService
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
from app.domain.ports.prediction_repository import PredictionRepositoryPort
from app.infrastructure.config import Settings, get_settings
from app.infrastructure.http.purchase_sale_client import PurchaseSaleClient
from app.infrastructure.http.transaction_loader import HttpTransactionLoader
from app.infrastructure.http.vehicle_client import VehicleClient
from app.infrastructure.ml.feature_engineering import FeatureEngineering
from app.infrastructure.ml.model_training import ModelTrainer
from app.infrastructure.persistence.database import (
    close_engine,
    database_enabled,
    get_session_factory,
    init_engine,
)
from app.infrastructure.persistence.db_model_registry import DatabaseModelRegistry
from app.infrastructure.persistence.feature_repository import FeatureRepository
from app.infrastructure.persistence.fs_model_registry import FileSystemModelRegistry
from app.infrastructure.persistence.prediction_repository import PredictionRepository

logger = logging.getLogger(__name__)

# Indicadores que apuntan a recursos de producción.
_PROD_ENV_VALUES = {"prod", "production", "prd"}
_PROD_HOST_MARKERS = ("prod", "rds", "aws", "sgivu-postgres")


def _abort_if_production(settings: Settings) -> None:
    """Aborta si la configuración apunta al entorno de producción.

    Verifica ``environment``, ``database_env`` y el host de la BD para
    detectar configuración de producción. Los scripts de dev nunca deben
    conectarse a recursos productivos.
    """
    env = (settings.environment or "").strip().lower()
    db_env = (settings.database_env or env).strip().lower()

    is_prod_env = env in _PROD_ENV_VALUES or db_env in _PROD_ENV_VALUES

    prod_db_host = settings.prod_ml_db_host or ""
    dev_db_host = settings.dev_ml_db_host or ""
    active_host = prod_db_host if db_env in _PROD_ENV_VALUES else dev_db_host
    is_prod_host = any(marker in active_host.lower() for marker in _PROD_HOST_MARKERS)

    if not (is_prod_env or is_prod_host):
        return

    RED = "\033[1;31m"
    RESET = "\033[0m"
    print(
        f"\n{RED}{'█' * 66}{RESET}\n"
        f"{RED}█{'ACCESO A PRODUCCIÓN BLOQUEADO':^64}█{RESET}\n"
        f"{RED}█{RESET}{'':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  Los scripts de verificación del modelo solo pueden':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  ejecutarse contra el entorno de DESARROLLO.':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  Usa ./scripts/run_dev.sh — carga .env.dev':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  automáticamente y garantiza el entorno correcto.':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'':^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  ENVIRONMENT detectado: ' + env:^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'  DB host activo:        ' + active_host:^64}{RED}█{RESET}\n"
        f"{RED}█{RESET}{'':^64}{RED}█{RESET}\n"
        f"{RED}{'█' * 66}{RESET}\n",
        file=sys.stderr,
    )
    sys.exit(1)


async def build_prediction_service(
    settings: Settings | None = None,
) -> tuple[PredictionService, Callable]:
    """Construye el ``PredictionService`` con sus dependencias productivas.

    Replica el grafo de DI de ``app/api/dependencies.py``: selecciona el
    registry adecuado (base de datos o sistema de archivos), instancia los
    clientes HTTP y devuelve tanto el servicio como una función de limpieza.

    Parameters
    ----------
    settings : Settings | None
        Configuración de la aplicación. Si es ``None``, se obtiene con
        ``get_settings()`` (lee ``.env`` y variables de entorno).

    Returns
    -------
    tuple[PredictionService, Callable]
        Par ``(service, cleanup)``.  Llamar a ``await cleanup()`` cierra el
        engine de base de datos al terminar el script.
    """
    if settings is None:
        settings = get_settings()

    _abort_if_production(settings)

    registry: ModelRegistryPort
    feature_repository: FeatureRepositoryPort | None = None
    prediction_repository: PredictionRepositoryPort | None = None

    if database_enabled(settings):
        await init_engine(settings)
        factory = get_session_factory()
        assert factory is not None, "El engine se inicializó pero la factory es None."
        registry = DatabaseModelRegistry(factory, settings.model_name)
        feature_repository = FeatureRepository(factory)
        prediction_repository = PredictionRepository(factory)
        logger.info("Usando DatabaseModelRegistry (BD: %s)", settings.database_dsn())
    else:
        registry = FileSystemModelRegistry(settings)
        logger.info(
            "BD no configurada. Usando FileSystemModelRegistry en '%s'.",
            settings.model_dir,
        )

    purchase_sale_url = settings.sgivu_purchase_sale_url
    vehicle_url = settings.sgivu_vehicle_url
    logger.info("sgivu-purchase-sale → %s", purchase_sale_url)
    logger.info("sgivu-vehicle       → %s", vehicle_url)
    if "sgivu-purchase-sale" in purchase_sale_url or "sgivu-vehicle" in vehicle_url:
        logger.warning(
            "Las URLs apuntan a hostnames Docker internos y no son accesibles desde "
            "el host. Configura SGIVU_PURCHASE_SALE_URL y SGIVU_VEHICLE_URL en el "
            "archivo .env de apps/ml/sgivu-ml/ apuntando a localhost, por ejemplo:\n"
            "  SGIVU_PURCHASE_SALE_URL=http://localhost:8084\n"
            "  SGIVU_VEHICLE_URL=http://localhost:8083"
        )

    purchase_client = PurchaseSaleClient(settings)
    vehicle_client = VehicleClient(settings)
    transaction_loader = HttpTransactionLoader(purchase_client, vehicle_client)

    feature_engineering = FeatureEngineering(settings)
    model_trainer = ModelTrainer(settings)

    training_service = TrainingService(
        registry=registry,
        feature_engineering=feature_engineering,
        model_trainer=model_trainer,
        feature_repository=feature_repository,
        settings=settings,
    )

    service = PredictionService(
        registry=registry,
        feature_engineering=feature_engineering,
        training_service=training_service,
        transaction_loader=transaction_loader,
        feature_repository=feature_repository,
        prediction_repository=prediction_repository,
        settings=settings,
    )

    async def cleanup() -> None:
        await close_engine()

    return service, cleanup
