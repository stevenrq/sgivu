"""Tests unitarios para PredictionService.

Cubre los casos de uso: predecir, predecir-con-historial,
reentrenar y consultar metadata del modelo.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock

import pandas as pd
import pytest

from app.application.services.prediction_service import PredictionService
from app.domain.entities import (
    ModelMetadata,
    PredictionResult,
    PredictionWithHistoryResult,
)
from app.domain.exceptions import (
    DataLoadError,
    InsufficientHistoryError,
    MissingVehicleLineError,
    ModelNotTrainedError,
    SegmentNotFoundError,
)


# ------------------------------------------------------------------
# Fixture local: instancia del servicio bajo test
# ------------------------------------------------------------------


@pytest.fixture
def service(
    mock_model_registry: AsyncMock,
    mock_feature_engineering: MagicMock,
    mock_training_service: AsyncMock,
    mock_transaction_loader: AsyncMock,
    mock_feature_repository: AsyncMock,
    mock_prediction_repository: AsyncMock,
    mock_settings,
) -> PredictionService:
    """PredictionService con todas las dependencias mockeadas."""
    return PredictionService(
        registry=mock_model_registry,
        feature_engineering=mock_feature_engineering,
        training_service=mock_training_service,
        transaction_loader=mock_transaction_loader,
        feature_repository=mock_feature_repository,
        prediction_repository=mock_prediction_repository,
        settings=mock_settings,
    )


@pytest.fixture
def valid_filters() -> dict:
    """Filtros de predicción válidos y canónicos."""
    return {
        "vehicle_type": "CAR",
        "brand": "TOYOTA",
        "model": "COROLLA",
        "line": "XLE",
    }


# ===================================================================
class TestPredictionService:
    """PredictionService"""

    # ---------------------------------------------------------------
    class TestPredict:
        """predict(filters, horizon, confidence)"""

        @pytest.mark.asyncio
        async def test_returns_prediction_result_when_history_exists(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict(valid_filters, horizon=2)

            assert isinstance(result, PredictionResult)
            assert len(result.predictions) == 2
            assert result.model_version == "20250101120000"
            assert result.metrics is not None

        @pytest.mark.asyncio
        async def test_returns_correct_demand_from_model(
            self,
            service: PredictionService,
            valid_filters: dict,
            mock_model: MagicMock,
        ) -> None:
            result = await service.predict(valid_filters, horizon=1)

            assert result.predictions[0].demand == 13.0
            mock_model.predict.assert_called()

        @pytest.mark.asyncio
        async def test_raises_model_not_trained_when_no_model(
            self,
            service: PredictionService,
            valid_filters: dict,
            mock_model_registry: AsyncMock,
        ) -> None:
            mock_model_registry.load_latest.side_effect = FileNotFoundError(
                "No model found"
            )

            with pytest.raises(ModelNotTrainedError):
                await service.predict(valid_filters, horizon=3)

        @pytest.mark.asyncio
        async def test_raises_data_load_error_when_no_data_source(
            self,
            mock_model_registry: AsyncMock,
            mock_feature_engineering: MagicMock,
            mock_training_service: AsyncMock,
            mock_settings,
            valid_filters: dict,
        ) -> None:
            svc = PredictionService(
                registry=mock_model_registry,
                feature_engineering=mock_feature_engineering,
                training_service=mock_training_service,
                transaction_loader=None,
                feature_repository=None,
                settings=mock_settings,
            )

            with pytest.raises(DataLoadError):
                await svc.predict(valid_filters, horizon=3)

        @pytest.mark.asyncio
        async def test_raises_missing_vehicle_line_when_line_empty(
            self,
            service: PredictionService,
        ) -> None:
            filters = {
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "",
            }

            with pytest.raises(MissingVehicleLineError):
                await service.predict(filters, horizon=3)

        @pytest.mark.asyncio
        async def test_raises_segment_not_found_when_history_empty(
            self,
            service: PredictionService,
            valid_filters: dict,
            mock_feature_repository: AsyncMock,
            mock_transaction_loader: AsyncMock,
            mock_feature_engineering: MagicMock,
        ) -> None:
            mock_feature_repository.load_segment_history.return_value = pd.DataFrame()
            mock_transaction_loader.load_transactions.return_value = pd.DataFrame()
            mock_feature_engineering.build_feature_table.return_value = pd.DataFrame()

            with pytest.raises(SegmentNotFoundError):
                await service.predict(valid_filters, horizon=3)

        @pytest.mark.asyncio
        async def test_stores_prediction_when_repository_available(
            self,
            service: PredictionService,
            valid_filters: dict,
            mock_prediction_repository: AsyncMock,
        ) -> None:
            await service.predict(valid_filters, horizon=1)

            mock_prediction_repository.save_prediction.assert_awaited_once()

        @pytest.mark.asyncio
        async def test_computes_confidence_intervals(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict(valid_filters, horizon=1, confidence=0.95)

            point = result.predictions[0]
            assert point.lower_ci < point.demand
            assert point.upper_ci > point.demand
            assert point.lower_ci >= 0.0

    # ---------------------------------------------------------------
    class TestPredictWithHistory:
        """predict_with_history(filters, horizon, confidence)"""

        @pytest.mark.asyncio
        async def test_returns_predictions_and_history_points(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict_with_history(valid_filters, horizon=2)

            assert isinstance(result, PredictionWithHistoryResult)
            assert len(result.predictions) == 2
            assert len(result.history) == 3

        @pytest.mark.asyncio
        async def test_includes_segment_in_result(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict_with_history(valid_filters, horizon=1)

            assert result.segment.vehicle_type == "CAR"
            assert result.segment.brand == "TOYOTA"
            assert result.segment.model == "COROLLA"
            assert result.segment.line == "XLE"

        @pytest.mark.asyncio
        async def test_history_points_match_input_data(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict_with_history(valid_filters, horizon=1)

            assert result.history[0].month == "2024-10-01"
            assert result.history[0].sales_count == 10.0
            assert result.history[2].month == "2024-12-01"
            assert result.history[2].sales_count == 11.0

        @pytest.mark.asyncio
        async def test_includes_model_metadata(
            self,
            service: PredictionService,
            valid_filters: dict,
        ) -> None:
            result = await service.predict_with_history(valid_filters, horizon=1)

            assert result.model_version == "20250101120000"
            assert result.trained_at == "2025-01-01T12:00:00+00:00"
            assert result.metrics is not None

        @pytest.mark.asyncio
        async def test_stores_prediction_with_history_flag(
            self,
            service: PredictionService,
            valid_filters: dict,
            mock_prediction_repository: AsyncMock,
        ) -> None:
            await service.predict_with_history(valid_filters, horizon=1)

            call_kwargs = mock_prediction_repository.save_prediction.call_args.kwargs
            assert call_kwargs["with_history"] is True

    # ---------------------------------------------------------------
    class TestRetrain:
        """retrain(start_date, end_date)"""

        @pytest.mark.asyncio
        async def test_returns_metadata_when_training_succeeds(
            self,
            service: PredictionService,
            sample_metadata: ModelMetadata,
        ) -> None:
            result = await service.retrain()

            assert isinstance(result, ModelMetadata)
            assert result.version == sample_metadata.version

        @pytest.mark.asyncio
        async def test_delegates_to_training_service(
            self,
            service: PredictionService,
            mock_training_service: AsyncMock,
        ) -> None:
            await service.retrain()

            mock_training_service.train.assert_awaited_once()

        @pytest.mark.asyncio
        async def test_raises_data_load_error_when_no_transaction_loader(
            self,
            mock_model_registry: AsyncMock,
            mock_feature_engineering: MagicMock,
            mock_training_service: AsyncMock,
            mock_settings,
        ) -> None:
            svc = PredictionService(
                registry=mock_model_registry,
                feature_engineering=mock_feature_engineering,
                training_service=mock_training_service,
                transaction_loader=None,
                settings=mock_settings,
            )

            with pytest.raises(DataLoadError):
                await svc.retrain()

        @pytest.mark.asyncio
        async def test_raises_insufficient_history_when_data_empty(
            self,
            service: PredictionService,
            mock_transaction_loader: AsyncMock,
        ) -> None:
            mock_transaction_loader.load_transactions.return_value = pd.DataFrame()

            with pytest.raises(InsufficientHistoryError):
                await service.retrain()

        @pytest.mark.asyncio
        async def test_passes_date_range_to_loader(
            self,
            service: PredictionService,
            mock_transaction_loader: AsyncMock,
        ) -> None:
            from datetime import date

            start = date(2024, 1, 1)
            end = date(2024, 12, 31)

            await service.retrain(start_date=start, end_date=end)

            mock_transaction_loader.load_transactions.assert_awaited_once_with(
                start_date=start, end_date=end
            )

    # ---------------------------------------------------------------
    class TestGetLatestModel:
        """get_latest_model()"""

        @pytest.mark.asyncio
        async def test_returns_metadata_when_model_exists(
            self,
            service: PredictionService,
            sample_metadata: ModelMetadata,
        ) -> None:
            result = await service.get_latest_model()

            assert result is not None
            assert result.version == sample_metadata.version

        @pytest.mark.asyncio
        async def test_returns_none_when_no_model(
            self,
            service: PredictionService,
            mock_model_registry: AsyncMock,
        ) -> None:
            mock_model_registry.latest_metadata.return_value = None

            result = await service.get_latest_model()

            assert result is None

    # ---------------------------------------------------------------
    class TestNormalizeFilters:
        """_normalize_filters(filters)"""

        def test_returns_vehicle_segment_with_canonicalized_labels(
            self,
            service: PredictionService,
        ) -> None:
            filters = {
                "vehicle_type": "car",
                "brand": "toyota",
                "model": "corolla",
                "line": "xle",
            }

            segment = service._normalize_filters(filters)

            assert segment.vehicle_type == "CAR"
            assert segment.brand == "TOYOTA"
            assert segment.model == "COROLLA"
            assert segment.line == "XLE"

        def test_raises_missing_vehicle_line_when_line_is_none(
            self,
            service: PredictionService,
        ) -> None:
            filters = {
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": None,
            }

            with pytest.raises(MissingVehicleLineError):
                service._normalize_filters(filters)

        def test_raises_missing_vehicle_line_when_line_is_empty(
            self,
            service: PredictionService,
        ) -> None:
            filters = {
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "   ",
            }

            with pytest.raises(MissingVehicleLineError):
                service._normalize_filters(filters)

    # ---------------------------------------------------------------
    class TestZValue:
        """_z_value(confidence)"""

        @pytest.mark.parametrize(
            ("confidence", "expected"),
            [
                (0.99, 2.58),
                (0.95, 1.96),
                (0.90, 1.64),
                (0.80, 1.28),
                (0.70, 1.0),
            ],
        )
        def test_returns_correct_z_for_confidence_levels(
            self, confidence: float, expected: float
        ) -> None:
            assert PredictionService._z_value(confidence) == expected

    # ---------------------------------------------------------------
    class TestLoadHistory:
        """_load_history(segment, model_version)"""

        @pytest.mark.asyncio
        async def test_returns_from_feature_repo_when_available(
            self,
            service: PredictionService,
            sample_history_df: pd.DataFrame,
            mock_feature_repository: AsyncMock,
        ) -> None:
            from app.domain.entities import VehicleSegment

            segment = VehicleSegment(
                vehicle_type="CAR",
                brand="TOYOTA",
                model="COROLLA",
                line="XLE",
            )

            result = await service._load_history(segment, "v1")

            assert not result.empty
            assert len(result) == len(sample_history_df)
            mock_feature_repository.load_segment_history.assert_awaited_once()

        @pytest.mark.asyncio
        async def test_falls_back_to_loader_when_repo_returns_empty(
            self,
            service: PredictionService,
            mock_feature_repository: AsyncMock,
            mock_transaction_loader: AsyncMock,
            mock_feature_engineering: MagicMock,
        ) -> None:
            from app.domain.entities import VehicleSegment

            mock_feature_repository.load_segment_history.return_value = pd.DataFrame()

            segment = VehicleSegment(
                vehicle_type="CAR",
                brand="TOYOTA",
                model="COROLLA",
                line="XLE",
            )

            await service._load_history(segment, "v1")

            mock_transaction_loader.load_transactions.assert_awaited_once()
            mock_feature_engineering.build_feature_table.assert_called_once()

        @pytest.mark.asyncio
        async def test_returns_empty_when_no_loader_and_repo_empty(
            self,
            mock_model_registry: AsyncMock,
            mock_feature_engineering: MagicMock,
            mock_training_service: AsyncMock,
            mock_feature_repository: AsyncMock,
            mock_settings,
        ) -> None:
            from app.domain.entities import VehicleSegment

            mock_feature_repository.load_segment_history.return_value = pd.DataFrame()

            svc = PredictionService(
                registry=mock_model_registry,
                feature_engineering=mock_feature_engineering,
                training_service=mock_training_service,
                transaction_loader=None,
                feature_repository=mock_feature_repository,
                settings=mock_settings,
            )

            segment = VehicleSegment(
                vehicle_type="CAR",
                brand="TOYOTA",
                model="COROLLA",
                line="XLE",
            )

            result = await svc._load_history(segment, "v1")

            assert result.empty
