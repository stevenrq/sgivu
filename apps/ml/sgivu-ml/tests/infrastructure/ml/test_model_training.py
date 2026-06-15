"""Tests de entrenamiento y evaluación de modelos."""

from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from app.infrastructure.config import Settings
from app.infrastructure.ml.model_training import ModelTrainer


@pytest.fixture
def settings() -> Settings:
    return Settings(
        model_dir="/tmp/sgivu_test_models",
        min_history_months=1,
        target_column="sales_count",
        sgivu_purchase_sale_url="http://test",
        sgivu_vehicle_url="http://test",
        enable_hyperparameter_tuning=False,
        model_selection_metric="rmse",
        residual_shrinkage_prior=5,
    )


@pytest.fixture
def tuning_settings() -> Settings:
    return Settings(
        model_dir="/tmp/sgivu_test_models",
        min_history_months=1,
        target_column="sales_count",
        sgivu_purchase_sale_url="http://test",
        sgivu_vehicle_url="http://test",
        enable_hyperparameter_tuning=True,
        cv_folds=2,
        cv_n_iter=2,
        model_selection_metric="weighted",
        model_selection_mape_weight=0.4,
        residual_shrinkage_prior=5,
    )


def _training_dataset(months: int = 6) -> pd.DataFrame:
    """Dataset de entrenamiento sintético con features ya procesadas."""
    rng = np.random.RandomState(42)
    rows = []
    for i in range(months):
        month = i + 1
        rows.append(
            {
                "event_month": pd.Timestamp(f"2024-{month:02d}-01"),
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "XLE",
                "sales_count": float(10 + rng.randint(-3, 4)),
                "purchases_count": float(8 + rng.randint(-2, 3)),
                "avg_margin": 5000.0 + rng.randn() * 500,
                "avg_sale_price": 25000.0,
                "avg_purchase_price": 20000.0,
                "avg_days_inventory": 30.0,
                "inventory_rotation": 1.2,
                "lag_1": float(max(0, 10 + rng.randint(-3, 4))),
                "lag_3": 0.0,
                "lag_6": 0.0,
                "rolling_mean_3": 10.0,
                "rolling_mean_6": 10.0,
                "month": month,
                "year": 2024,
                "month_sin": float(np.sin(2 * np.pi * month / 12)),
                "month_cos": float(np.cos(2 * np.pi * month / 12)),
                "quarter": (month - 1) // 3 + 1,
                "quarter_sin": float(np.sin(2 * np.pi * ((month - 1) // 3 + 1) / 4)),
                "quarter_cos": float(np.cos(2 * np.pi * ((month - 1) // 3 + 1) / 4)),
            }
        )
    return pd.DataFrame(rows)


class TestTrainAndEvaluate:
    """train_and_evaluate()"""

    def test_shouldTrainAndReturnEvaluation(self, settings: Settings) -> None:
        """Debe entrenar y retornar evaluación con métricas."""
        trainer = ModelTrainer(settings)
        dataset = _training_dataset(6)
        category_cols = ["vehicle_type", "brand", "model", "line"]
        numeric_cols = [
            "purchases_count",
            "avg_margin",
            "avg_sale_price",
            "avg_purchase_price",
            "avg_days_inventory",
            "inventory_rotation",
            "lag_1",
            "lag_3",
            "lag_6",
            "rolling_mean_3",
            "rolling_mean_6",
            "month",
            "year",
            "month_sin",
            "month_cos",
            "quarter",
            "quarter_sin",
            "quarter_cos",
        ]
        result = trainer.train_and_evaluate(dataset, category_cols, [], numeric_cols)
        assert result.pipeline is not None
        assert "rmse" in result.metrics
        assert "mae" in result.metrics
        assert "mape" in result.metrics
        assert result.train_samples > 0
        assert result.test_samples > 0

    def test_shouldNotIncludeLinearRegressionInCandidates(
        self, settings: Settings
    ) -> None:
        """Debe no incluir LinearRegression en los candidatos."""
        trainer = ModelTrainer(settings)
        candidates = trainer._candidate_models()
        names = [name for name, _ in candidates]
        assert "linear_regression" not in names
        assert "random_forest" in names

    def test_shouldRaiseValueErrorWhenInsufficientHistory(self) -> None:
        """Debe lanzar ValueError si hay menos meses de los requeridos."""
        s = Settings(
            model_dir="/tmp/test",
            min_history_months=12,
            target_column="sales_count",
            sgivu_purchase_sale_url="http://test",
            sgivu_vehicle_url="http://test",
            enable_hyperparameter_tuning=False,
        )
        trainer = ModelTrainer(s)
        dataset = _training_dataset(6)
        with pytest.raises(ValueError, match="al menos 12 meses"):
            trainer.train_and_evaluate(
                dataset, ["vehicle_type", "brand", "model", "line"], [], []
            )

    def test_shouldIncludeSegmentResidualsInEvaluation(
        self, settings: Settings
    ) -> None:
        """Debe incluir residuales por segmento en la evaluación."""
        trainer = ModelTrainer(settings)
        dataset = _training_dataset(6)
        category_cols = ["vehicle_type", "brand", "model", "line"]
        numeric_cols = [
            "purchases_count",
            "avg_margin",
            "avg_sale_price",
            "avg_purchase_price",
            "avg_days_inventory",
            "inventory_rotation",
            "lag_1",
            "lag_3",
            "lag_6",
            "rolling_mean_3",
            "rolling_mean_6",
            "month",
            "year",
            "month_sin",
            "month_cos",
            "quarter",
            "quarter_sin",
            "quarter_cos",
        ]
        result = trainer.train_and_evaluate(dataset, category_cols, [], numeric_cols)
        assert isinstance(result.segment_residuals, dict)

    def test_shouldComputeHorizonResidualsPerStep(self, settings: Settings) -> None:
        """Debe calcular horizon_residuals por paso cuando el dataset tiene
        horizon_step."""
        trainer = ModelTrainer(settings)
        base = _training_dataset(8)
        category_cols = ["vehicle_type", "brand", "model", "line"]
        direct_numeric_cols = [
            "purchases_count",
            "avg_margin",
            "avg_sale_price",
            "avg_purchase_price",
            "avg_days_inventory",
            "inventory_rotation",
            "lag_1",
            "lag_3",
            "lag_6",
            "rolling_mean_3",
            "rolling_mean_6",
            "month",
            "year",
            "month_sin",
            "month_cos",
            "quarter",
            "quarter_sin",
            "quarter_cos",
            "horizon_step",
        ]

        rows = []
        for _, row in base.iterrows():
            for h in range(1, 4):
                new_row = row.to_dict()
                new_row["horizon_step"] = float(h)
                new_row["sales_count"] = row["sales_count"] + h
                rows.append(new_row)
        direct_dataset = pd.DataFrame(rows)

        result = trainer.train_and_evaluate(
            direct_dataset, category_cols, [], direct_numeric_cols
        )

        assert isinstance(result.horizon_residuals, dict)
        assert len(result.horizon_residuals) > 0
        for h, std in result.horizon_residuals.items():
            assert isinstance(h, int)
            assert std >= 0.0


class TestTrainWithTuning:
    """train_and_evaluate() con hyperparameter tuning"""

    def test_shouldTrainWithTimeSeriesSplitWhenTuningEnabled(
        self, tuning_settings: Settings
    ) -> None:
        """Debe entrenar con TimeSeriesSplit cuando el tuning está habilitado."""
        trainer = ModelTrainer(tuning_settings)
        dataset = _training_dataset(8)
        category_cols = ["vehicle_type", "brand", "model", "line"]
        numeric_cols = [
            "purchases_count",
            "avg_margin",
            "avg_sale_price",
            "avg_purchase_price",
            "avg_days_inventory",
            "inventory_rotation",
            "lag_1",
            "lag_3",
            "lag_6",
            "rolling_mean_3",
            "rolling_mean_6",
            "month",
            "year",
            "month_sin",
            "month_cos",
            "quarter",
            "quarter_sin",
            "quarter_cos",
        ]
        result = trainer.train_and_evaluate(dataset, category_cols, [], numeric_cols)
        assert result.pipeline is not None
        assert "rmse" in result.metrics


class TestSelectionScore:
    """_selection_score()"""

    def test_shouldSelectByWeightedMetricWhenConfigured(self) -> None:
        """Debe usar métrica ponderada RMSE+WAPE."""
        s = Settings(
            model_dir="/tmp/test",
            min_history_months=1,
            target_column="sales_count",
            sgivu_purchase_sale_url="http://test",
            sgivu_vehicle_url="http://test",
            model_selection_metric="weighted",
            model_selection_mape_weight=0.4,
            enable_hyperparameter_tuning=False,
        )
        trainer = ModelTrainer(s)
        metrics = {"rmse": 2.0, "wape": 0.5}
        score = trainer._selection_score(metrics)
        expected = 0.6 * 2.0 + 0.4 * 0.5
        assert abs(score - expected) < 1e-6

    def test_shouldSelectByRmseWhenConfigured(self) -> None:
        """Debe usar RMSE puro como criterio."""
        s = Settings(
            model_dir="/tmp/test",
            min_history_months=1,
            target_column="sales_count",
            sgivu_purchase_sale_url="http://test",
            sgivu_vehicle_url="http://test",
            model_selection_metric="rmse",
            enable_hyperparameter_tuning=False,
        )
        trainer = ModelTrainer(s)
        metrics = {"rmse": 2.0, "mape": 0.5}
        assert trainer._selection_score(metrics) == 2.0

    def test_shouldSelectByMapeWhenConfigured(self) -> None:
        """Debe usar MAPE puro como criterio."""
        s = Settings(
            model_dir="/tmp/test",
            min_history_months=1,
            target_column="sales_count",
            sgivu_purchase_sale_url="http://test",
            sgivu_vehicle_url="http://test",
            model_selection_metric="mape",
            enable_hyperparameter_tuning=False,
        )
        trainer = ModelTrainer(s)
        metrics = {"rmse": 2.0, "mape": 0.5}
        assert trainer._selection_score(metrics) == 0.5
