"""Entrenamiento y evaluación de modelos de ML.

Responsabilidad única: ajustar, evaluar y seleccionar el mejor pipeline
de sklearn/xgboost.  No realiza I/O ni persistencia.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import (
    mean_absolute_error,
    mean_absolute_percentage_error,
    mean_squared_error,
    r2_score,
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.infrastructure.config import Settings

try:
    from xgboost import XGBRegressor
except Exception:  # pragma: no cover
    XGBRegressor = None  # type: ignore[assignment,misc]

logger = logging.getLogger(__name__)


@dataclass
class TrainingEvaluation:
    """Resultado de la evaluación de modelos candidatos."""

    pipeline: Any
    metrics: dict[str, float]
    residual_std: float
    candidates: list[dict[str, Any]]
    train_samples: int
    test_samples: int


class ModelTrainer:
    """Entrena, evalúa y selecciona el mejor modelo de demanda.

    Clase sin estado ni I/O; recibe un DataFrame y devuelve la evaluación.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def train_and_evaluate(
        self,
        dataset: pd.DataFrame,
        category_cols: list[str],
        optional_category_cols: list[str],
        numeric_cols: list[str],
    ) -> TrainingEvaluation:
        """Divide, entrena candidatos, evalúa y devuelve el mejor pipeline.

        Raises:
            ValueError: si el historial tiene menos meses de los requeridos.
        """
        train_df, test_df = self._split_by_time(dataset)

        optional_cols = [c for c in optional_category_cols if c in dataset.columns]
        feature_cols = category_cols + optional_cols + numeric_cols

        x_train = train_df[feature_cols]
        y_train = train_df["sales_count"]
        x_test = test_df[feature_cols]
        y_test = test_df["sales_count"]

        preprocessor = self._build_preprocessor(
            category_cols, optional_cols, numeric_cols
        )
        candidates_list = self._candidate_models()

        evaluated: list[dict[str, Any]] = []
        best_model: Pipeline | None = None
        best_rmse = np.inf
        best_metrics: dict[str, float] = {}
        best_predictions: np.ndarray | None = None
        sample_count = len(y_test)

        def _safe_metric(value: float) -> float:
            return float(value) if np.isfinite(value) else 0.0

        for name, estimator in candidates_list:
            pipeline = Pipeline(
                steps=[("preprocess", preprocessor), ("model", estimator)],
                memory=None,
            )
            pipeline.fit(x_train, y_train)
            preds = np.asarray(pipeline.predict(x_test))
            rmse = _safe_metric(np.sqrt(mean_squared_error(y_test, preds)))
            mae = _safe_metric(mean_absolute_error(y_test, preds))
            safe_y_test = y_test.replace(0, 1e-3)
            mape = _safe_metric(mean_absolute_percentage_error(safe_y_test, preds))
            if sample_count >= 2:
                r2 = _safe_metric(r2_score(y_test, preds))
            else:
                logger.debug("Skipping r2: only %s sample(s) in test", sample_count)
                r2 = 0.0

            evaluated.append(
                {
                    "model": name,
                    "rmse": rmse,
                    "mae": mae,
                    "mape": mape,
                    "r2": r2,
                    "samples": sample_count,
                }
            )

            if rmse < best_rmse:
                best_rmse = rmse
                best_model = pipeline
                best_metrics = {"rmse": rmse, "mae": mae, "mape": mape, "r2": r2}
                best_predictions = preds

        assert best_model is not None

        # Refit en el dataset completo para el modelo final.
        final_model = best_model.fit(dataset[feature_cols], dataset["sales_count"])

        residuals = y_test - best_predictions if best_predictions is not None else []
        residual_std = float(np.std(residuals)) if len(residuals) else 1.0

        return TrainingEvaluation(
            pipeline=final_model,
            metrics=best_metrics,
            residual_std=residual_std,
            candidates=evaluated,
            train_samples=len(train_df),
            test_samples=len(test_df),
        )

    def _split_by_time(self, df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame]:
        """Divide en train/test respetando el orden temporal del historial."""
        unique_months = sorted(df["event_month"].unique())
        if len(unique_months) < self._settings.min_history_months:
            raise ValueError(
                f"Se requieren al menos {self._settings.min_history_months} meses "
                f"para entrenar."
            )
        if len(unique_months) == 1:
            return df.copy(), df.copy()
        cutoff_index = int(len(unique_months) * 0.8)
        cutoff_date = unique_months[max(1, cutoff_index - 1)]
        train = df[df["event_month"] <= cutoff_date]
        test = df[df["event_month"] > cutoff_date]
        if test.empty:
            test = train.tail(max(1, len(train) // 5))
            train = train.drop(test.index)
        return train, test

    @staticmethod
    def _build_preprocessor(
        category_cols: list[str],
        optional_cols: list[str],
        numeric_cols: list[str],
    ) -> ColumnTransformer:
        """Construye el ColumnTransformer con encoding categórico y escalado numérico."""
        categorical = OneHotEncoder(handle_unknown="ignore")
        numeric = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
            ]
        )
        return ColumnTransformer(
            transformers=[
                ("categorical", categorical, category_cols + optional_cols),
                ("numeric", numeric, numeric_cols),
            ],
            remainder="drop",
        )

    @staticmethod
    def _candidate_models() -> list[tuple[str, Any]]:
        """Devuelve la lista de modelos candidatos a evaluar."""
        models: list[tuple[str, Any]] = [
            ("linear_regression", LinearRegression()),
            (
                "random_forest",
                RandomForestRegressor(n_estimators=300, max_depth=15, random_state=7),
            ),
        ]
        if XGBRegressor:
            models.append(
                (
                    "xgboost",
                    XGBRegressor(
                        n_estimators=500,
                        max_depth=6,
                        learning_rate=0.05,
                        subsample=0.9,
                        colsample_bytree=0.9,
                        objective="reg:squarederror",
                        random_state=7,
                    ),
                )
            )
        return models
