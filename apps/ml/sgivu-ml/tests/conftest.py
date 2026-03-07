"""Fixtures compartidos para los tests del microservicio sgivu-ml."""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock

import numpy as np
import pandas as pd
import pytest

from app.domain.entities import ModelMetadata
from app.infrastructure.config import Settings
from app.infrastructure.ml.model_training import TrainingEvaluation


# ------------------------------------------------------------------
# Configuración
# ------------------------------------------------------------------


@pytest.fixture
def mock_settings() -> Settings:
    """Configuración con valores mínimos controlados para tests."""
    return Settings(
        model_dir="/tmp/sgivu_test_models",
        min_history_months=1,
        target_column="sales_count",
        sgivu_purchase_sale_url="http://test-purchase-sale",
        sgivu_vehicle_url="http://test-vehicle",
        permissions_predict=["ml:predict"],
        permissions_retrain=["ml:retrain"],
        permissions_models=["ml:models"],
    )


# ------------------------------------------------------------------
# Entidades de dominio
# ------------------------------------------------------------------


@pytest.fixture
def sample_metadata() -> ModelMetadata:
    """Metadata representativa de un modelo entrenado."""
    return ModelMetadata(
        version="20250101120000",
        trained_at="2025-01-01T12:00:00+00:00",
        target="sales_count",
        features=[
            "vehicle_type",
            "brand",
            "model",
            "line",
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
        ],
        metrics={
            "rmse": 1.5,
            "mae": 1.0,
            "mape": 0.15,
            "r2": 0.85,
            "residual_std": 1.2,
        },
        candidates=[{"model": "linear_regression", "rmse": 2.0}],
        train_samples=80,
        test_samples=20,
        total_samples=100,
    )


# ------------------------------------------------------------------
# DataFrames de prueba
# ------------------------------------------------------------------


@pytest.fixture
def sample_history_df() -> pd.DataFrame:
    """DataFrame de historial con 3 meses para un solo segmento."""
    months = pd.date_range("2024-10-01", periods=3, freq="MS")
    return pd.DataFrame(
        {
            "event_month": months,
            "vehicle_type": ["CAR"] * 3,
            "brand": ["TOYOTA"] * 3,
            "model": ["COROLLA"] * 3,
            "line": ["XLE"] * 3,
            "sales_count": [10.0, 12.0, 11.0],
            "purchases_count": [8.0, 10.0, 9.0],
            "avg_margin": [5000.0, 5500.0, 5200.0],
            "avg_sale_price": [25000.0, 26000.0, 25500.0],
            "avg_purchase_price": [20000.0, 20500.0, 20300.0],
            "avg_days_inventory": [30.0, 28.0, 29.0],
            "inventory_rotation": [1.25, 1.2, 1.22],
            "lag_1": [0.0, 10.0, 12.0],
            "lag_3": [0.0, 0.0, 0.0],
            "lag_6": [0.0, 0.0, 0.0],
            "rolling_mean_3": [0.0, 10.0, 11.0],
            "rolling_mean_6": [0.0, 10.0, 11.0],
            "month": [10, 11, 12],
            "year": [2024, 2024, 2024],
            "month_sin": [np.sin(2 * np.pi * m / 12) for m in [10, 11, 12]],
            "month_cos": [np.cos(2 * np.pi * m / 12) for m in [10, 11, 12]],
        }
    )


@pytest.fixture
def sample_future_row() -> pd.DataFrame:
    """Fila de features de un mes futuro (resultado de build_future_row)."""
    return pd.DataFrame(
        [
            {
                "event_month": pd.Timestamp("2025-01-01"),
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "XLE",
                "purchases_count": 9.0,
                "avg_margin": 5200.0,
                "avg_sale_price": 25500.0,
                "avg_purchase_price": 20300.0,
                "avg_days_inventory": 29.0,
                "inventory_rotation": 1.22,
                "lag_1": 11.0,
                "lag_3": 10.0,
                "lag_6": 0.0,
                "rolling_mean_3": 11.0,
                "rolling_mean_6": 11.0,
                "month": 1,
                "year": 2025,
                "month_sin": np.sin(2 * np.pi / 12),
                "month_cos": np.cos(2 * np.pi / 12),
            }
        ]
    )


# ------------------------------------------------------------------
# Mocks de modelos sklearn
# ------------------------------------------------------------------


@pytest.fixture
def mock_model() -> MagicMock:
    """Mock de un pipeline sklearn con predict()."""
    model = MagicMock()
    model.predict.return_value = np.array([13.0])
    return model


# ------------------------------------------------------------------
# Mocks de puertos (ports)
# ------------------------------------------------------------------


@pytest.fixture
def mock_model_registry(
    sample_metadata: ModelMetadata, mock_model: MagicMock
) -> AsyncMock:
    """Mock del puerto ModelRegistryPort."""
    registry = AsyncMock()
    registry.load_latest.return_value = (mock_model, sample_metadata)
    registry.save.return_value = sample_metadata
    registry.latest_metadata.return_value = sample_metadata
    return registry


@pytest.fixture
def mock_feature_repository(sample_history_df: pd.DataFrame) -> AsyncMock:
    """Mock del puerto FeatureRepositoryPort."""
    repo = AsyncMock()
    repo.load_segment_history.return_value = sample_history_df
    repo.save_snapshot.return_value = None
    repo.load_snapshot.return_value = sample_history_df
    return repo


@pytest.fixture
def mock_prediction_repository() -> AsyncMock:
    """Mock del puerto PredictionRepositoryPort."""
    repo = AsyncMock()
    repo.save_prediction.return_value = None
    return repo


@pytest.fixture
def mock_transaction_loader(sample_history_df: pd.DataFrame) -> AsyncMock:
    """Mock del puerto TransactionLoaderPort."""
    loader = AsyncMock()
    loader.load_transactions.return_value = sample_history_df
    return loader


# ------------------------------------------------------------------
# Mocks de infraestructura ML
# ------------------------------------------------------------------


@pytest.fixture
def mock_feature_engineering(
    sample_history_df: pd.DataFrame,
    sample_future_row: pd.DataFrame,
) -> MagicMock:
    """Mock de FeatureEngineering con atributos de columnas y métodos simulados."""
    fe = MagicMock()
    fe.category_cols = ["vehicle_type", "brand", "model", "line"]
    fe.optional_category_cols = []
    fe.numeric_cols = [
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
    ]
    fe.build_future_row.return_value = sample_future_row
    fe.build_feature_table.return_value = sample_history_df
    return fe


@pytest.fixture
def mock_model_trainer(mock_model: MagicMock) -> MagicMock:
    """Mock de ModelTrainer con evaluación predefinida."""
    trainer = MagicMock()
    trainer.train_and_evaluate.return_value = TrainingEvaluation(
        pipeline=mock_model,
        metrics={"rmse": 1.5, "mae": 1.0, "mape": 0.15, "r2": 0.85},
        residual_std=1.2,
        candidates=[{"model": "linear_regression", "rmse": 2.0}],
        train_samples=80,
        test_samples=20,
    )
    return trainer


# ------------------------------------------------------------------
# Mocks de servicios de aplicación
# ------------------------------------------------------------------


@pytest.fixture
def mock_training_service(sample_metadata: ModelMetadata) -> AsyncMock:
    """Mock de TrainingService."""
    svc = AsyncMock()
    svc.train.return_value = sample_metadata
    return svc
