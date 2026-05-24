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

        Orquesta el flujo completo: construcción de features, evaluación de
        candidatos mediante `ModelTrainer` (posible tuning) y persistencia
        del artefacto y snapshot de features.

        Parameters
        ----------
        raw_df : pd.DataFrame
            DataFrame de transacciones brutas (sin preprocesar).

        Returns
        -------
        ModelMetadata
            Metadata del modelo recién entrenado y versionado.

        Raises
        ------
        TrainingError
            Si los datos son insuficientes o si ocurre un fallo durante
            el proceso de entrenamiento/evaluación.
        """
        try:
            dataset = self._feature_engineering.build_feature_table(raw_df)
        except ValueError as exc:
            raise TrainingError(str(exc)) from exc

        if dataset.empty:
            raise TrainingError("No hay datos históricos para entrenar.")

        direct_dataset = self._feature_engineering.build_direct_feature_table(
            dataset, self._settings.max_training_horizon
        )
        if direct_dataset.empty:
            raise TrainingError(
                "No hay suficientes datos para construir el dataset multi-step directo."
            )

        try:
            evaluation = await asyncio.to_thread(
                self._model_trainer.train_and_evaluate,
                direct_dataset,
                self._feature_engineering.category_cols,
                self._feature_engineering.optional_category_cols,
                self._feature_engineering.direct_numeric_cols,
            )
        except ValueError as exc:
            raise TrainingError(str(exc)) from exc

        metadata_dict: dict[str, Any] = {
            "trained_at": datetime.now(timezone.utc).isoformat(),
            "target": self._settings.target_column,
            "features": (
                self._feature_engineering.category_cols
                + self._feature_engineering.direct_numeric_cols
            ),
            "metrics": {
                **evaluation.metrics,
                "residual_std": evaluation.residual_std,
                "segment_residuals": evaluation.segment_residuals,
                "horizon_residuals": evaluation.horizon_residuals,
                "feature_importances": evaluation.feature_importances,
            },
            "candidates": evaluation.candidates,
            "best_params": evaluation.best_params,
            "feature_importances": evaluation.feature_importances,
            "train_samples": evaluation.train_samples,
            "test_samples": evaluation.test_samples,
            "total_samples": len(direct_dataset),
        }

        saved = await self._registry.save(evaluation.pipeline, metadata_dict)

        if self._feature_repository:
            await self._feature_repository.save_snapshot(saved.version, dataset)

        logger.info("Model trained and versioned: %s", saved.version)
        return saved
