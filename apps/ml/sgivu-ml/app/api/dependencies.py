"""Fábricas de dependencias para FastAPI (``Depends``).

Cadena de inyección de dependencias que conecta puertos de dominio
con implementaciones concretas de infraestructura.
Cada fábrica recibe sus propias dependencias vía ``Depends``,
logrando un grafo declarativo de DI.
"""

from __future__ import annotations

from fastapi import Depends

from app.application.services.monitoring_service import MonitoringService
from app.application.services.prediction_service import PredictionService
from app.application.services.training_service import TrainingService
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
from app.domain.ports.monitoring_repository import MonitoringRepositoryPort
from app.domain.ports.prediction_repository import PredictionRepositoryPort
from app.domain.ports.transaction_loader import TransactionLoaderPort
from app.infrastructure.config import Settings, get_settings
from app.infrastructure.http.purchase_sale_client import PurchaseSaleClient
from app.infrastructure.http.transaction_loader import HttpTransactionLoader
from app.infrastructure.http.vehicle_client import VehicleClient
from app.infrastructure.ml.feature_engineering import FeatureEngineering
from app.infrastructure.ml.model_training import ModelTrainer
from app.infrastructure.persistence.database import (
    database_enabled,
    get_session_factory,
)
from app.infrastructure.persistence.db_model_registry import DatabaseModelRegistry
from app.infrastructure.persistence.feature_repository import FeatureRepository
from app.infrastructure.persistence.fs_model_registry import FileSystemModelRegistry
from app.infrastructure.persistence.monitoring_repository import MonitoringRepository
from app.infrastructure.persistence.prediction_repository import PredictionRepository

# ------------------------------------------------------------------
# Infraestructura
# ------------------------------------------------------------------


def get_model_registry(
    settings: Settings = Depends(get_settings),
) -> ModelRegistryPort:
    """Selecciona la implementación de registry según la configuración de BD."""
    factory = get_session_factory()
    if factory is not None:
        return DatabaseModelRegistry(factory, settings.model_name)
    return FileSystemModelRegistry(settings)


def get_feature_repository(
    settings: Settings = Depends(get_settings),
) -> FeatureRepositoryPort | None:
    """Devuelve el repositorio de features si la BD está configurada."""
    if not database_enabled(settings):
        return None
    factory = get_session_factory()
    if factory is None:
        return None
    return FeatureRepository(factory)


def get_prediction_repository(
    settings: Settings = Depends(get_settings),
) -> PredictionRepositoryPort | None:
    """Devuelve el repositorio de predicciones si la BD está configurada."""
    if not database_enabled(settings):
        return None
    factory = get_session_factory()
    if factory is None:
        return None
    return PredictionRepository(factory)


def get_transaction_loader(
    settings: Settings = Depends(get_settings),
) -> TransactionLoaderPort:
    """Construye el cargador de transacciones HTTP."""
    purchase_client = PurchaseSaleClient(settings)
    vehicle_client = VehicleClient(settings)
    return HttpTransactionLoader(purchase_client, vehicle_client)


def get_feature_engineering(
    settings: Settings = Depends(get_settings),
) -> FeatureEngineering:
    """Crea y devuelve la instancia de `FeatureEngineering`.

    Parameters
    ----------
    settings : Settings
        Configuración inyectada de la aplicación.

    Returns
    -------
    FeatureEngineering
        Instancia utilizada para construir features en pipelines.
    """
    return FeatureEngineering(settings)


def get_model_trainer(
    settings: Settings = Depends(get_settings),
) -> ModelTrainer:
    """Construye el `ModelTrainer` con la configuración provista.

    Parameters
    ----------
    settings : Settings
        Configuración de la aplicación.

    Returns
    -------
    ModelTrainer
        Objeto responsable del entrenamiento y evaluación de modelos.
    """
    return ModelTrainer(settings)


# ------------------------------------------------------------------
# Servicios de aplicación
# ------------------------------------------------------------------


def get_training_service(
    registry: ModelRegistryPort = Depends(get_model_registry),
    feature_engineering: FeatureEngineering = Depends(get_feature_engineering),
    model_trainer: ModelTrainer = Depends(get_model_trainer),
    feature_repository: FeatureRepositoryPort | None = Depends(get_feature_repository),
    settings: Settings = Depends(get_settings),
) -> TrainingService:
    """Construye el servicio de entrenamiento de la aplicación.

    Parameters
    ----------
    registry : ModelRegistryPort
        Puerto para persistir y recuperar artefactos de modelos.
    feature_engineering : FeatureEngineering
        Utilidad para generar features.
    model_trainer : ModelTrainer
        Componente que entrena y evalúa modelos.
    feature_repository : FeatureRepositoryPort | None
        Repositorio de features si la BD está configurada.
    settings : Settings
        Configuración de la aplicación.

    Returns
    -------
    TrainingService
        Servicio de aplicación que orquesta el entrenamiento.
    """
    return TrainingService(
        registry=registry,
        feature_engineering=feature_engineering,
        model_trainer=model_trainer,
        feature_repository=feature_repository,
        settings=settings,
    )


def get_monitoring_repository(
    settings: Settings = Depends(get_settings),
) -> MonitoringRepositoryPort | None:
    """Devuelve el repositorio de monitoreo si la BD está configurada."""
    if not database_enabled(settings):
        return None
    factory = get_session_factory()
    if factory is None:
        return None
    return MonitoringRepository(factory)


def get_monitoring_service(
    monitoring_repository: MonitoringRepositoryPort | None = Depends(
        get_monitoring_repository
    ),
    prediction_repository: PredictionRepositoryPort | None = Depends(
        get_prediction_repository
    ),
) -> MonitoringService | None:
    """Construye el servicio de monitoreo si el repositorio está disponible.

    Parameters
    ----------
    monitoring_repository : MonitoringRepositoryPort | None
        Repositorio para persistir métricas de monitoreo.
    prediction_repository : PredictionRepositoryPort | None
        Repositorio de predicciones históricas.

    Returns
    -------
    MonitoringService | None
        Instancia del servicio o `None` si la BD no está configurada.
    """
    if monitoring_repository is None:
        return None
    return MonitoringService(
        monitoring_repository=monitoring_repository,
        prediction_repository=prediction_repository,
    )


def get_prediction_service(
    registry: ModelRegistryPort = Depends(get_model_registry),
    feature_engineering: FeatureEngineering = Depends(get_feature_engineering),
    training_service: TrainingService = Depends(get_training_service),
    transaction_loader: TransactionLoaderPort = Depends(get_transaction_loader),
    feature_repository: FeatureRepositoryPort | None = Depends(get_feature_repository),
    prediction_repository: PredictionRepositoryPort | None = Depends(
        get_prediction_repository
    ),
    settings: Settings = Depends(get_settings),
) -> PredictionService:
    """Construye el `PredictionService` con todas sus dependencias.

    Parameters
    ----------
    registry : ModelRegistryPort
        Puerto para acceder a los artefactos de modelos.
    feature_engineering : FeatureEngineering
        Generador de features.
    training_service : TrainingService
        Servicio de entrenamiento (para reentrenamientos programados).
    transaction_loader : TransactionLoaderPort
        Cliente para cargar transacciones desde el backend.
    feature_repository : FeatureRepositoryPort | None
        Repositorio de features (si está disponible).
    prediction_repository : PredictionRepositoryPort | None
        Repositorio de predicciones históricas (si está disponible).
    settings : Settings
        Configuración de la aplicación.

    Returns
    -------
    PredictionService
        Servicio de aplicación que expone operaciones de predicción.
    """
    return PredictionService(
        registry=registry,
        feature_engineering=feature_engineering,
        training_service=training_service,
        transaction_loader=transaction_loader,
        feature_repository=feature_repository,
        prediction_repository=prediction_repository,
        settings=settings,
    )
