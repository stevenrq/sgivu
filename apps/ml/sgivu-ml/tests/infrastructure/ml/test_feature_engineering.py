"""Tests de ingeniería de features."""

from __future__ import annotations

import pandas as pd
import pytest

from app.infrastructure.config import Settings
from app.infrastructure.ml.feature_engineering import FeatureEngineering


@pytest.fixture
def settings() -> Settings:
    return Settings(
        model_dir="/tmp/sgivu_test_models",
        min_history_months=1,
        target_column="sales_count",
        sgivu_purchase_sale_url="http://test",
        sgivu_vehicle_url="http://test",
        outlier_iqr_multiplier=3.0,
        cold_start_min_months=3,
        enable_hyperparameter_tuning=False,
    )


@pytest.fixture
def fe(settings: Settings) -> FeatureEngineering:
    return FeatureEngineering(settings)


def _raw_transactions(months: int = 6) -> pd.DataFrame:
    """Genera transacciones brutas sintéticas para N meses."""
    rows = []
    for i in range(months):
        date = pd.Timestamp(f"2024-{i + 1:02d}-15")
        rows.append(
            {
                "vehicle_type": "CAR",
                "brand": "Toyota",
                "model": "Corolla",
                "line": "XLE",
                "contract_type": "SALE",
                "sale_price": 25000.0,
                "purchase_price": 20000.0,
                "vehicle_id": i + 100,
                "created_at": date,
                "updated_at": date,
            }
        )
        rows.append(
            {
                "vehicle_type": "CAR",
                "brand": "Toyota",
                "model": "Corolla",
                "line": "XLE",
                "contract_type": "PURCHASE",
                "sale_price": 0.0,
                "purchase_price": 20000.0,
                "vehicle_id": i + 100,
                "created_at": date - pd.Timedelta(days=10),
                "updated_at": date - pd.Timedelta(days=10),
            }
        )
    return pd.DataFrame(rows)


class TestBuildFeatureTable:
    """build_feature_table()"""

    def test_shouldComputeCorrectMonthlyAggregations(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe agregar transacciones por mes y segmento correctamente."""
        raw = _raw_transactions(3)
        result = fe.build_feature_table(raw)
        assert not result.empty
        assert "sales_count" in result.columns
        assert "event_month" in result.columns
        assert len(result) == 3

    def test_shouldReturnZeroInventoryRotationWhenNoPurchases(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe retornar 0 en inventory_rotation cuando no hay compras."""
        raw = _raw_transactions(3)
        raw = raw[raw["contract_type"] == "SALE"].copy()
        result = fe.build_feature_table(raw)
        assert (result["inventory_rotation"] == 0.0).all()

    def test_shouldRaiseValueErrorWhenLineColumnMissing(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe lanzar ValueError si falta la columna 'line'."""
        raw = _raw_transactions(3)
        raw = raw.drop(columns=["line"])
        with pytest.raises(ValueError, match="line"):
            fe.build_feature_table(raw)

    def test_raises_value_error_when_line_contains_missing_alias(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe rechazar líneas con alias faltantes como N/D tras normalizar."""
        raw = _raw_transactions(3)
        raw["line"] = "N/D"
        with pytest.raises(ValueError, match="línea"):
            fe.build_feature_table(raw)

    def test_shouldComputeCorrectLagsPerSegment(self, fe: FeatureEngineering) -> None:
        """Debe calcular lags correctamente por segmento."""
        raw = _raw_transactions(6)
        result = fe.build_feature_table(raw)
        result = result.sort_values("event_month")
        assert result["lag_1"].iloc[0] == 0.0
        assert result["lag_1"].iloc[1] > 0.0

    def test_shouldCanonicalizeCategories(self, fe: FeatureEngineering) -> None:
        """Debe normalizar categorías a mayúsculas sin caracteres especiales."""
        raw = _raw_transactions(3)
        result = fe.build_feature_table(raw)
        assert (result["brand"] == "TOYOTA").all()
        assert (result["model"] == "COROLLA").all()

    def test_shouldHandleEmptyDataFrame(self, fe: FeatureEngineering) -> None:
        """Debe retornar DataFrame vacío si la entrada está vacía."""
        result = fe.build_feature_table(pd.DataFrame())
        assert result.empty

    def test_shouldParseIso8601WithAndWithoutFractionalSeconds(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe parsear timestamps ISO8601 mezclados con y sin microsegundos."""
        raw = pd.DataFrame(
            [
                {
                    "vehicle_type": "CAR",
                    "brand": "Toyota",
                    "model": "Corolla",
                    "line": "XLE",
                    "contract_type": "SALE",
                    "sale_price": 25000.0,
                    "purchase_price": 20000.0,
                    "vehicle_id": 1,
                    "created_at": "2024-01-15T10:00:00",
                    "updated_at": "2024-01-15T10:00:00.668838",
                },
                {
                    "vehicle_type": "CAR",
                    "brand": "Toyota",
                    "model": "Corolla",
                    "line": "XLE",
                    "contract_type": "PURCHASE",
                    "sale_price": 0.0,
                    "purchase_price": 20000.0,
                    "vehicle_id": 1,
                    "created_at": "2024-01-01T08:00:00.000001",
                    "updated_at": "2024-01-01T08:00:00",
                },
            ]
        )

        result = fe.build_feature_table(raw)

        assert not result.empty
        assert (result["event_month"] == pd.Timestamp("2024-01-01")).all()


class TestBuildFutureRow:
    """build_future_row()"""

    def test_shouldBuildFutureRowWithCorrectFeatures(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe construir fila futura con todas las features esperadas."""
        raw = _raw_transactions(6)
        history = fe.build_feature_table(raw)
        target = history["event_month"].max() + pd.offsets.MonthBegin(1)
        future = fe.build_future_row(history, target)

        assert len(future) == 1
        for col in fe.category_cols + fe.numeric_cols:
            assert col in future.columns

    def test_shouldRaiseValueErrorWhenHistoryEmpty(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe lanzar ValueError si el historial está vacío."""
        with pytest.raises(ValueError, match="historial"):
            fe.build_future_row(pd.DataFrame(), pd.Timestamp("2025-01-01"))


class TestBuildDirectFeatureTable:
    """build_direct_feature_table()"""

    def test_shouldGenerateHorizonRowsPerObservation(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe generar hasta max_horizon filas por cada observación con
        futuro."""
        raw = _raw_transactions(6)
        monthly = fe.build_feature_table(raw)
        direct = fe.build_direct_feature_table(monthly, max_horizon=3)

        assert not direct.empty
        assert "horizon_step" in direct.columns
        assert set(direct["horizon_step"].astype(int).unique()).issubset({1, 2, 3})

    def test_shouldAnchorLagsAtOriginTime(self, fe: FeatureEngineering) -> None:
        """Los lags en cada fila deben ser los del mes t, no del mes t+h."""
        raw = _raw_transactions(6)
        monthly = fe.build_feature_table(raw)
        direct = fe.build_direct_feature_table(monthly, max_horizon=2)

        h1_rows = direct[direct["horizon_step"] == 1.0]
        h2_rows = direct[direct["horizon_step"] == 2.0]

        # Para el mismo event_month de origen, los lags deben ser idénticos
        # independientemente del horizonte
        common_months = set(h1_rows["event_month"]) & set(h2_rows["event_month"])
        for month in common_months:
            lag_h1 = h1_rows[h1_rows["event_month"] == month]["lag_1"].values[0]
            lag_h2 = h2_rows[h2_rows["event_month"] == month]["lag_1"].values[0]
            assert lag_h1 == lag_h2

    def test_shouldReturnEmptyWhenNoFutureDataAvailable(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe retornar vacío si el dataset tiene solo 1 mes (sin futuro)."""
        raw = _raw_transactions(1)
        monthly = fe.build_feature_table(raw)
        direct = fe.build_direct_feature_table(monthly, max_horizon=3)
        assert direct.empty


class TestBuildFutureRowAtOrigin:
    """build_future_row_at_origin()"""

    def test_shouldBuildRowWithDirectNumericCols(self, fe: FeatureEngineering) -> None:
        """Debe construir fila con todas las direct_numeric_cols incluido
        horizon_step."""
        raw = _raw_transactions(6)
        history = fe.build_feature_table(raw)
        target = history["event_month"].max() + pd.offsets.MonthBegin(1)
        row = fe.build_future_row_at_origin(history, target, horizon_step=3)

        assert len(row) == 1
        assert "horizon_step" in row.columns
        assert int(row["horizon_step"].iloc[0]) == 3
        for col in fe.direct_numeric_cols:
            assert col in row.columns

    def test_shouldUseFrozenLagsFromActualHistory(self, fe: FeatureEngineering) -> None:
        """Los lags deben coincidir con los del último mes real del historial."""
        raw = _raw_transactions(6)
        history = fe.build_feature_table(raw).sort_values("event_month")
        last_actual_lag1 = history["sales_count"].iloc[-1]

        target = history["event_month"].max() + pd.offsets.MonthBegin(2)
        row = fe.build_future_row_at_origin(history, target, horizon_step=2)

        assert abs(row["lag_1"].iloc[0] - last_actual_lag1) < 1e-9

    def test_shouldRaiseValueErrorWhenHistoryEmpty(
        self, fe: FeatureEngineering
    ) -> None:
        """Debe lanzar ValueError si el historial está vacío."""
        with pytest.raises(ValueError, match="historial"):
            fe.build_future_row_at_origin(
                pd.DataFrame(), pd.Timestamp("2025-01-01"), horizon_step=1
            )
