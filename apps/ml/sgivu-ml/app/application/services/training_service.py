"""Servicio de entrenamiento de modelos de demanda.

Orquesta el flujo: feature engineering → entrenamiento ML →
persistencia del modelo vía puertos.  No contiene lógica de ML directa
(delegada a ``FeatureEngineering`` y ``ModelTrainer``) ni acceso directo
a la base de datos.
"""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import Any

import pandas as pd

from app.domain.entities import ModelMetadata
from app.domain.exceptions import TrainingError
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
from app.infrastructure.config import Settings, get_settings
from app.infrastructure.ml.feature_engineering import FeatureEngineering
from app.infrastructure.ml.model_training import ModelTrainer

logger = logging.getLogger(__name__)


class TrainingService:
    """Caso de uso: entrenar y versionar un modelo de demanda."""

    def __init__(
        self,
        registry: ModelRegistryPort,
        feature_engineering: FeatureEngineering,
        model_trainer: ModelTrainer,
        feature_repository: FeatureRepositoryPort | None = None,
        settings: Settings | None = None,
    ) -> None:
        self._registry = registry
        self._feature_engineering = feature_engineering
        self._model_trainer = model_trainer
        self._feature_repository = feature_repository
        self._settings = settings or get_settings()

    async def train(self, raw_df: pd.DataFrame) -> ModelMetadata:
        """Entrena con datos brutos y persiste el mejor modelo.

        Raises:
            TrainingError: si los datos son insuficientes o el entrenamiento falla.
        """
        try:
            dataset = self._feature_engineering.build_feature_table(raw_df)
        except ValueError as exc:
            raise TrainingError(str(exc)) from exc

        if dataset.empty:
            raise TrainingError("No hay datos históricos para entrenar.")

        try:
            evaluation = await asyncio.to_thread(
                self._model_trainer.train_and_evaluate,
                dataset,
                self._feature_engineering.category_cols,
                self._feature_engineering.optional_category_cols,
                self._feature_engineering.numeric_cols,
            )
        except ValueError as exc:
            raise TrainingError(str(exc)) from exc

        metadata_dict: dict[str, Any] = {
            "trained_at": datetime.now(timezone.utc).isoformat(),
            "target": self._settings.target_column,
            "features": (
                self._feature_engineering.category_cols
                + self._feature_engineering.numeric_cols
            ),
            "metrics": {
                **evaluation.metrics,
                "residual_std": evaluation.residual_std,
            },
            "candidates": evaluation.candidates,
            "train_samples": evaluation.train_samples,
            "test_samples": evaluation.test_samples,
            "total_samples": len(dataset),
        }

        saved = await self._registry.save(evaluation.pipeline, metadata_dict)

        if self._feature_repository:
            await self._feature_repository.save_snapshot(saved.version, dataset)

        logger.info("Model trained and versioned: %s", saved.version)
        return saved
