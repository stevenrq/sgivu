"""Tests de extracción de importancias de features."""

from __future__ import annotations

from unittest.mock import MagicMock

import numpy as np
import pytest

from app.infrastructure.ml.feature_importance import extract_importance


def _mock_pipeline(importances: list[float], transformed_names: list[str]) -> MagicMock:
    """Crea un mock de Pipeline con un modelo que tiene feature_importances_."""
    model = MagicMock()
    model.feature_importances_ = np.array(importances)

    preprocessor = MagicMock()
    preprocessor.get_feature_names_out.return_value = transformed_names

    pipeline = MagicMock()
    pipeline.named_steps = {"preprocess": preprocessor, "model": model}
    return pipeline


class TestExtractImportance:
    """extract_importance()"""

    def test_shouldExtractImportancesFromRandomForest(self) -> None:
        """Debe extraer y normalizar importancias de un RandomForest."""
        feature_names = ["brand", "lag_1", "month"]
        transformed_names = [
            "categorical__brand_TOYOTA",
            "categorical__brand_HONDA",
            "numeric__lag_1",
            "numeric__month",
        ]
        importances = [0.2, 0.1, 0.5, 0.2]

        pipeline = _mock_pipeline(importances, transformed_names)
        result = extract_importance(pipeline, feature_names)

        assert "brand" in result
        assert "lag_1" in result
        assert "month" in result
        assert abs(sum(result.values()) - 1.0) < 1e-6

    def test_shouldReturnEmptyDictForUnsupportedModel(self) -> None:
        """Debe retornar dict vacío si el modelo no tiene feature_importances_."""
        model = MagicMock(spec=[])
        del model.feature_importances_

        pipeline = MagicMock()
        pipeline.named_steps = {"preprocess": MagicMock(), "model": model}

        result = extract_importance(pipeline, ["lag_1"])
        assert not result

    def test_shouldReturnEmptyDictWhenNoPipeline(self) -> None:
        """Debe retornar dict vacío si no es un Pipeline válido."""
        pipeline = MagicMock()
        pipeline.named_steps = {}

        result = extract_importance(pipeline, ["lag_1"])
        assert not result

    def test_shouldAggregateOneHotEncodedFeatures(self) -> None:
        """Debe sumar importancias de columnas one-hot del mismo feature."""
        feature_names = ["vehicle_type", "lag_1"]
        transformed_names = [
            "categorical__vehicle_type_CAR",
            "categorical__vehicle_type_MOTORCYCLE",
            "numeric__lag_1",
        ]
        importances = [0.2, 0.3, 0.5]

        pipeline = _mock_pipeline(importances, transformed_names)
        result = extract_importance(pipeline, feature_names)

        assert result["vehicle_type"] == pytest.approx(0.5)
        assert result["lag_1"] == pytest.approx(0.5)
