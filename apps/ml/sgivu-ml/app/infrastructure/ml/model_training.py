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
from sklearn.metrics import (
    make_scorer,
    mean_absolute_error,
    mean_absolute_percentage_error,
    mean_squared_error,
    r2_score,
)
from sklearn.model_selection import RandomizedSearchCV, TimeSeriesSplit
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.infrastructure.config import Settings
from app.infrastructure.ml.feature_importance import extract_importance

try:
    from xgboost import XGBRegressor
except Exception:  # pragma: no cover
    XGBRegressor = None  # type: ignore[assignment,misc]

logger = logging.getLogger(__name__)


@dataclass
class TrainingEvaluation:
    """Resultado de la evaluación de modelos candidatos."""

    pipeline: Any
    metrics: dict[str, Any]
    residual_std: float
    candidates: list[dict[str, Any]]
    train_samples: int
    test_samples: int
    segment_residuals: dict[str, float] = field(default_factory=dict)
    best_params: dict[str, Any] = field(default_factory=dict)
    feature_importances: dict[str, float] = field(default_factory=dict)
    horizon_residuals: dict[int, float] = field(default_factory=dict)


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
        """Divide los datos por tiempo, entrena candidatos y devuelve el mejor pipeline.

        Esta función realiza los pasos estándar de entrenamiento para series
        temporales: split temporal, preprocesado, entrenamiento (con o sin
        tuning), evaluación y refit del mejor candidato sobre el dataset
        completo.

        Parameters
        ----------
        dataset : pd.DataFrame
            DataFrame preprocesado que contiene la columna objetivo ``sales_count``.
        category_cols : list[str]
            Lista de columnas categóricas a codificar.
        optional_category_cols : list[str]
            Columnas categóricas opcionales que pueden o no existir en el dataset.
        numeric_cols : list[str]
            Columnas numéricas a incluir en el entrenamiento.

        Returns
        -------
        TrainingEvaluation
            Objeto con el pipeline final reentrenado, métricas, desviaciones
            residuales y metadatos relevantes.

        Raises
        ------
        ValueError
            Si el historial contiene menos meses de los requeridos por
            configuración (`min_history_months`).
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

        if self._settings.enable_hyperparameter_tuning:
            result = self._train_with_tuning(
                preprocessor, x_train, y_train, x_test, y_test
            )
        else:
            result = self._train_fixed_params(
                preprocessor, x_train, y_train, x_test, y_test
            )

        best_model = result["best_model"]
        best_metrics = result["best_metrics"]
        best_predictions = result["best_predictions"]
        evaluated = result["evaluated"]
        best_params = result.get("best_params", {})

        # Baselines naive sobre el mismo test set: el modelo solo aporta valor
        # real si su RMSE es estrictamente menor que el de ambos baselines.
        best_metrics["baselines"] = self._compute_baselines(test_df, y_test)

        # Refit en el dataset completo para el modelo final.
        final_model = best_model.fit(dataset[feature_cols], dataset["sales_count"])

        residuals = y_test - best_predictions if best_predictions is not None else []
        residual_std = float(np.std(residuals)) if len(residuals) else 1.0

        segment_residuals = self._compute_segment_residuals(
            test_df, best_predictions, residual_std, category_cols
        )

        horizon_residuals = self._compute_horizon_residuals(
            test_df, best_predictions, residual_std
        )

        feature_importances: dict[str, float] = {}
        if self._settings.enable_feature_importance:
            feature_importances = extract_importance(final_model, feature_cols)

        return TrainingEvaluation(
            pipeline=final_model,
            metrics=best_metrics,
            residual_std=residual_std,
            candidates=evaluated,
            train_samples=len(train_df),
            test_samples=len(test_df),
            segment_residuals=segment_residuals,
            best_params=best_params,
            feature_importances=feature_importances,
            horizon_residuals=horizon_residuals,
        )

    def _train_fixed_params(
        self,
        preprocessor: ColumnTransformer,
        x_train: pd.DataFrame,
        y_train: pd.Series,
        x_test: pd.DataFrame,
        y_test: pd.Series,
    ) -> dict[str, Any]:
        """Entrena candidatos con hiperparámetros fijos (modo rápido)."""
        candidates_list = self._candidate_models()
        evaluated: list[dict[str, Any]] = []
        best_model: Pipeline | None = None
        best_score = np.inf
        best_metrics: dict[str, float] = {}
        best_predictions: np.ndarray | None = None
        sample_count = len(y_test)

        for name, estimator in candidates_list:
            pipeline = Pipeline(
                steps=[("preprocess", preprocessor), ("model", estimator)],
                memory=None,
            )
            pipeline.fit(x_train, y_train)
            preds = np.asarray(pipeline.predict(x_test))
            metrics = self._compute_metrics(y_test, preds, sample_count)

            evaluated.append({"model": name, **metrics, "samples": sample_count})

            score = self._selection_score(metrics)
            if score < best_score:
                best_score = score
                best_model = pipeline
                best_metrics = metrics
                best_predictions = preds

        assert best_model is not None
        return {
            "best_model": best_model,
            "best_metrics": best_metrics,
            "best_predictions": best_predictions,
            "evaluated": evaluated,
        }

    def _train_with_tuning(
        self,
        preprocessor: ColumnTransformer,
        x_train: pd.DataFrame,
        y_train: pd.Series,
        x_test: pd.DataFrame,
        y_test: pd.Series,
    ) -> dict[str, Any]:
        """Entrena candidatos con RandomizedSearchCV y TimeSeriesSplit."""
        candidates_list = self._candidate_models()
        grids = self._hyperparameter_grids()

        n_splits = min(self._settings.cv_folds, max(2, len(y_train) - 1))
        cv = TimeSeriesSplit(n_splits=n_splits)
        scorer = self._build_scorer()

        evaluated: list[dict[str, Any]] = []
        best_model: Pipeline | None = None
        best_score = np.inf
        best_metrics: dict[str, float] = {}
        best_predictions: np.ndarray | None = None
        best_params: dict[str, Any] = {}
        sample_count = len(y_test)

        for name, estimator in candidates_list:
            pipeline = Pipeline(
                steps=[("preprocess", preprocessor), ("model", estimator)],
                memory=None,
            )

            param_grid = grids.get(name)
            if param_grid:
                logger.info(
                    "Tuning %s with %d iterations...", name, self._settings.cv_n_iter
                )
                search = RandomizedSearchCV(
                    pipeline,
                    param_distributions=param_grid,
                    n_iter=min(self._settings.cv_n_iter, self._grid_size(param_grid)),
                    cv=cv,
                    scoring=scorer,
                    random_state=42,
                    n_jobs=self._settings.cv_n_jobs,
                    error_score="raise",
                )
                search.fit(x_train, y_train)
                pipeline = search.best_estimator_
                found_params = search.best_params_
            else:
                pipeline.fit(x_train, y_train)
                found_params = {}

            preds = np.asarray(pipeline.predict(x_test))
            metrics = self._compute_metrics(y_test, preds, sample_count)

            evaluated.append(
                {
                    "model": name,
                    **metrics,
                    "samples": sample_count,
                    "params": found_params,
                }
            )

            score = self._selection_score(metrics)
            if score < best_score:
                best_score = score
                best_model = pipeline
                best_metrics = metrics
                best_predictions = preds
                best_params = found_params

        assert best_model is not None
        return {
            "best_model": best_model,
            "best_metrics": best_metrics,
            "best_predictions": best_predictions,
            "evaluated": evaluated,
            "best_params": best_params,
        }

    def _selection_score(self, metrics: dict[str, float]) -> float:
        """Calcula el score de selección según la configuración.

        El modo "weighted" combina RMSE con WAPE en lugar de MAPE: WAPE es
        robusto a ceros en `sales_count` y por tanto refleja mejor la calidad
        del modelo cuando la demanda mensual de un segmento es escasa.
        """
        metric_type = self._settings.model_selection_metric
        if metric_type == "mape":
            return metrics.get("mape", np.inf)
        if metric_type == "weighted":
            wape_w = self._settings.model_selection_mape_weight
            rmse = metrics.get("rmse", np.inf)
            wape = metrics.get("wape", np.inf)
            return (1 - wape_w) * rmse + wape_w * wape
        return metrics.get("rmse", np.inf)

    def _build_scorer(self) -> Any:
        """Construye el scorer para cross-validation.

        El modo "weighted" usa WAPE (no MAPE) para evitar distorsiones cuando
        la serie objetivo contiene ceros, lo que es habitual en demanda mensual
        por segmento.
        """
        metric_type = self._settings.model_selection_metric
        if metric_type == "mape":
            return "neg_mean_absolute_percentage_error"
        if metric_type == "weighted":
            wape_w = self._settings.model_selection_mape_weight

            def _weighted_score(y_true: Any, y_pred: Any) -> float:
                rmse = float(np.sqrt(mean_squared_error(y_true, y_pred)))
                denom = float(np.sum(np.abs(y_true)))
                wape = (
                    float(
                        np.sum(np.abs(np.asarray(y_true) - np.asarray(y_pred))) / denom
                    )
                    if denom > 0
                    else 0.0
                )
                return -((1 - wape_w) * rmse + wape_w * wape)

            return make_scorer(_weighted_score, greater_is_better=True)
        return "neg_root_mean_squared_error"

    @staticmethod
    def _compute_baselines(
        test_df: pd.DataFrame, y_test: pd.Series
    ) -> dict[str, float]:
        """Calcula RMSE de baselines naive sobre el test set.

        - ``naive_lag1_rmse``: predecir el último valor observado (`lag_1`).
        - ``naive_mean3_rmse``: predecir la media móvil de 3 meses (`rolling_mean_3`).

        Estas baselines permiten cuantificar el valor real aportado por el
        modelo: si su RMSE no es menor que ambas, no aporta sobre una
        predicción trivial y debería rechazarse.
        """
        baselines: dict[str, float] = {}
        if "lag_1" in test_df.columns:
            lag1 = test_df["lag_1"].fillna(0.0).to_numpy()
            baselines["naive_lag1_rmse"] = float(
                np.sqrt(mean_squared_error(y_test, lag1))
            )
        if "rolling_mean_3" in test_df.columns:
            mean3 = test_df["rolling_mean_3"].fillna(0.0).to_numpy()
            baselines["naive_mean3_rmse"] = float(
                np.sqrt(mean_squared_error(y_test, mean3))
            )
        return baselines

    @staticmethod
    def _compute_metrics(
        y_test: pd.Series, preds: np.ndarray, sample_count: int
    ) -> dict[str, float]:
        """Calcula las métricas de evaluación del modelo.

        Incluye WAPE (Weighted Absolute Percentage Error) además de MAPE.
        WAPE es robusto a ceros en la serie objetivo: WAPE = Σ|y - ŷ| / Σ|y|.
        """

        def _safe(value: float) -> float:
            return float(value) if np.isfinite(value) else 0.0

        rmse = _safe(np.sqrt(mean_squared_error(y_test, preds)))
        mae = _safe(mean_absolute_error(y_test, preds))
        safe_y_test = y_test.replace(0, 1e-3)
        mape = _safe(mean_absolute_percentage_error(safe_y_test, preds))
        r2 = _safe(r2_score(y_test, preds)) if sample_count >= 2 else 0.0
        denom = float(np.sum(np.abs(y_test)))
        wape = (
            _safe(float(np.sum(np.abs(y_test.values - preds))) / denom)
            if denom > 0
            else 0.0
        )
        return {"rmse": rmse, "mae": mae, "mape": mape, "wape": wape, "r2": r2}

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
        """Construye ColumnTransformer con encoding categórico y escalado numérico."""
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

    def _compute_segment_residuals(
        self,
        test_df: pd.DataFrame,
        predictions: np.ndarray | None,
        global_std: float,
        category_cols: list[str],
    ) -> dict[str, float]:
        """Calcula residual_std por segmento con shrinkage Bayesiano.

        Mezcla la std del segmento con la global ponderada por el número
        de muestras para evitar estimaciones ruidosas en segmentos pequeños.
        """
        if predictions is None or len(predictions) == 0:
            return {}

        prior = self._settings.residual_shrinkage_prior
        work = test_df.copy()
        work["_residual"] = work["sales_count"].values - predictions

        segment_residuals: dict[str, float] = {}
        for keys, group in work.groupby(category_cols, sort=False):
            if len(group) < 2:
                continue
            seg_std = float(np.std(group["_residual"]))
            n = len(group)
            blended = (n * seg_std + prior * global_std) / (n + prior)
            key = "|".join(str(k) for k in keys)
            segment_residuals[key] = blended

        return segment_residuals

    def _compute_horizon_residuals(
        self,
        test_df: pd.DataFrame,
        predictions: np.ndarray | None,
        global_std: float,
    ) -> dict[int, float]:
        """Calcula residual_std por horizonte con shrinkage Bayesiano.

        Requiere que ``test_df`` contenga la columna ``horizon_step``
        (presente cuando se entrenó con ``build_direct_feature_table``).
        Si la columna no existe, retorna un dict vacío y el sistema usa el
        ``residual_std`` global como fallback.
        """
        if predictions is None or len(predictions) == 0:
            return {}
        if "horizon_step" not in test_df.columns:
            return {}

        prior = self._settings.residual_shrinkage_prior
        work = test_df.copy()
        work["_residual"] = work["sales_count"].values - predictions

        horizon_residuals: dict[int, float] = {}
        for h, group in work.groupby("horizon_step", sort=True):
            if len(group) < 2:
                continue
            h_std = float(np.std(group["_residual"]))
            n = len(group)
            blended = (n * h_std + prior * global_std) / (n + prior)
            horizon_residuals[int(h)] = blended

        return horizon_residuals

    @staticmethod
    def _candidate_models() -> list[tuple[str, Any]]:
        """Devuelve la lista de modelos candidatos a evaluar."""
        models: list[tuple[str, Any]] = [
            (
                "random_forest",
                RandomForestRegressor(
                    n_estimators=300,
                    max_depth=15,
                    min_samples_leaf=2,
                    random_state=42,
                ),
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
                        random_state=42,
                    ),
                )
            )
        return models

    @staticmethod
    def _hyperparameter_grids() -> dict[str, dict[str, list[Any]]]:
        """Distribuciones de hiperparámetros para RandomizedSearchCV."""
        grids: dict[str, dict[str, list[Any]]] = {
            "random_forest": {
                "model__n_estimators": [100, 200, 300, 500],
                "model__max_depth": [8, 12, 15, 20, None],
                "model__min_samples_split": [2, 5, 10],
                "model__min_samples_leaf": [1, 2, 4],
            },
        }
        if XGBRegressor:
            grids["xgboost"] = {
                "model__n_estimators": [200, 300, 500],
                "model__max_depth": [4, 6, 8],
                "model__learning_rate": [0.01, 0.05, 0.1],
                "model__subsample": [0.7, 0.8, 0.9],
                "model__colsample_bytree": [0.7, 0.8, 0.9],
            }
        return grids

    @staticmethod
    def _grid_size(grid: dict[str, list[Any]]) -> int:
        """Calcula el tamaño total del espacio de búsqueda."""
        size = 1
        for values in grid.values():
            size *= len(values)
        return size
