"""Fábricas de dependencias para FastAPI (``Depends``).

Cadena de inyección de dependencias que conecta puertos de dominio
con implementaciones concretas de infraestructura.
Cada fábrica recibe sus propias dependencias vía ``Depends``,
logrando un grafo declarativo de DI.
"""

from __future__ import annotations

from fastapi import Depends

from app.application.services.prediction_service import PredictionService
from app.application.services.training_service import TrainingService
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
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
    return FeatureEngineering(settings)


def get_model_trainer(
    settings: Settings = Depends(get_settings),
) -> ModelTrainer:
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
    return TrainingService(
        registry=registry,
        feature_engineering=feature_engineering,
        model_trainer=model_trainer,
        feature_repository=feature_repository,
        settings=settings,
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
    return PredictionService(
        registry=registry,
        feature_engineering=feature_engineering,
        training_service=training_service,
        transaction_loader=transaction_loader,
        feature_repository=feature_repository,
        prediction_repository=prediction_repository,
        settings=settings,
    )
