"""Tests de validación y limpieza de datos."""

from __future__ import annotations

import pandas as pd
import pytest

from app.infrastructure.ml.data_validation import clip_outliers, validate_raw_data


def _minimal_df(**overrides: object) -> pd.DataFrame:
    """DataFrame mínimo con todas las columnas requeridas."""
    base = {
        "vehicle_type": ["CAR"],
        "brand": ["TOYOTA"],
        "model": ["COROLLA"],
        "line": ["XLE"],
        "contract_type": ["SALE"],
        "sale_price": [25000.0],
        "purchase_price": [20000.0],
        "vehicle_id": [1],
        "created_at": [pd.Timestamp("2024-01-01")],
    }
    base.update(overrides)
    return pd.DataFrame(base)


class TestValidateRawData:
    """validate_raw_data()"""

    def test_shouldReturnCleanDataFrameWhenAllColumnsPresent(self) -> None:
        """Debe retornar DataFrame sin advertencias cuando todo está completo."""
        df = _minimal_df()
        result, warnings = validate_raw_data(df)
        assert not result.empty
        assert not warnings

    def test_shouldRaiseValueErrorWhenCriticalColumnMissing(self) -> None:
        """Debe lanzar ValueError si falta una columna requerida."""
        df = pd.DataFrame({"vehicle_type": ["CAR"], "brand": ["TOYOTA"]})
        with pytest.raises(ValueError, match="Columnas requeridas ausentes"):
            validate_raw_data(df)

    def test_shouldReturnWarningsForEmptyValues(self) -> None:
        """Debe advertir sobre valores vacíos en columnas categóricas."""
        df = _minimal_df(brand=[""])
        _, warnings = validate_raw_data(df)
        assert any("brand" in w for w in warnings)

    def test_shouldReturnWarningsForNullValues(self) -> None:
        """Debe advertir sobre valores nulos en columnas categóricas."""
        df = _minimal_df(line=[None])
        _, warnings = validate_raw_data(df)
        assert any("line" in w for w in warnings)


class TestClipOutliers:
    """clip_outliers()"""

    def test_shouldClipOutliersUsingIQR(self) -> None:
        """Debe recortar valores fuera del rango IQR."""
        df = pd.DataFrame({"sales": [1.0, 2.0, 3.0, 4.0, 5.0, 100.0]})
        result = clip_outliers(df, "sales", iqr_multiplier=1.5)
        assert result["sales"].max() < 100.0

    def test_shouldNotClipValuesWithinNormalRange(self) -> None:
        """Debe no alterar valores dentro del rango normal."""
        df = pd.DataFrame({"sales": [10.0, 11.0, 12.0, 13.0, 14.0]})
        result = clip_outliers(df, "sales", iqr_multiplier=3.0)
        pd.testing.assert_frame_equal(result, df)

    def test_shouldHandleMissingColumn(self) -> None:
        """Debe retornar DataFrame sin cambios si la columna no existe."""
        df = pd.DataFrame({"other": [1.0, 2.0]})
        result = clip_outliers(df, "sales", iqr_multiplier=3.0)
        pd.testing.assert_frame_equal(result, df)

    def test_shouldHandleZeroIQR(self) -> None:
        """Debe no recortar cuando IQR es cero (valores constantes)."""
        df = pd.DataFrame({"sales": [5.0, 5.0, 5.0, 5.0]})
        result = clip_outliers(df, "sales", iqr_multiplier=3.0)
        pd.testing.assert_frame_equal(result, df)
