from functools import lru_cache
from typing import cast

from app.core.config import Settings, get_settings
from app.database.database import database_enabled
from app.database.repositories import FeatureStore, PredictionStore
from app.services.data_loader import DemandDatasetLoader
from app.services.model_registry import DatabaseModelRegistry, ModelRegistry
from app.services.prediction import PredictionService
from app.services.training import TrainingService


@lru_cache
def _settings() -> Settings:
    return get_settings()


@lru_cache
def _registry() -> ModelRegistry:
    settings = _settings()
    if database_enabled(settings):
        return cast(ModelRegistry, DatabaseModelRegistry(settings))
    return ModelRegistry(settings)


@lru_cache
def _feature_store() -> FeatureStore | None:
    settings = _settings()
    if not database_enabled(settings):
        return None
    return FeatureStore(settings)


@lru_cache
def _prediction_store() -> PredictionStore | None:
    settings = _settings()
    if not database_enabled(settings):
        return None
    return PredictionStore(settings)


def get_loader() -> DemandDatasetLoader:
    return DemandDatasetLoader(_settings())


def get_trainer() -> TrainingService:
    return TrainingService(
        registry=_registry(),
        feature_store=_feature_store(),
        settings=_settings(),
    )


def get_prediction_service() -> PredictionService:
    return PredictionService(
        loader=get_loader(),
        trainer=get_trainer(),
        registry=_registry(),
        feature_store=_feature_store(),
        prediction_store=_prediction_store(),
        settings=_settings(),
    )
