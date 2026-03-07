"""Tests unitarios para TrainingService.

Cubre el caso de uso principal: entrenar un modelo de demanda a partir
de datos brutos, persistir el resultado y manejar errores.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pandas as pd
import pytest

from app.application.services.training_service import TrainingService
from app.domain.entities import ModelMetadata
from app.domain.exceptions import TrainingError


# ------------------------------------------------------------------
# Fixture local: instancia del servicio bajo test
# ------------------------------------------------------------------


@pytest.fixture
def service(
    mock_model_registry: AsyncMock,
    mock_feature_engineering: MagicMock,
    mock_model_trainer: MagicMock,
    mock_feature_repository: AsyncMock,
    mock_settings,
) -> TrainingService:
    """TrainingService con todas las dependencias mockeadas."""
    return TrainingService(
        registry=mock_model_registry,
        feature_engineering=mock_feature_engineering,
        model_trainer=mock_model_trainer,
        feature_repository=mock_feature_repository,
        settings=mock_settings,
    )


@pytest.fixture
def raw_df(sample_history_df: pd.DataFrame) -> pd.DataFrame:
    """DataFrame bruto de entrada (reutiliza el fixture de historial)."""
    return sample_history_df.copy()


# ===================================================================
class TestTrainingService:
    """TrainingService"""

    # ---------------------------------------------------------------
    class TestTrain:
        """train(raw_df)"""

        @pytest.mark.asyncio
        async def test_returns_model_metadata_when_training_succeeds(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            sample_metadata: ModelMetadata,
        ) -> None:
            result = await service.train(raw_df)

            assert isinstance(result, ModelMetadata)
            assert result.version == sample_metadata.version

        @pytest.mark.asyncio
        async def test_calls_feature_engineering_with_raw_data(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_feature_engineering: MagicMock,
        ) -> None:
            await service.train(raw_df)

            mock_feature_engineering.build_feature_table.assert_called_once()
            call_arg = mock_feature_engineering.build_feature_table.call_args[0][0]
            assert isinstance(call_arg, pd.DataFrame)

        @pytest.mark.asyncio
        async def test_delegates_training_to_model_trainer(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_model_trainer: MagicMock,
        ) -> None:
            await service.train(raw_df)

            mock_model_trainer.train_and_evaluate.assert_called_once()

        @pytest.mark.asyncio
        async def test_saves_model_via_registry(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_model_registry: AsyncMock,
        ) -> None:
            await service.train(raw_df)

            mock_model_registry.save.assert_awaited_once()
            call_args = mock_model_registry.save.call_args
            metadata_dict = call_args[0][1]
            assert "trained_at" in metadata_dict
            assert "metrics" in metadata_dict
            assert "target" in metadata_dict

        @pytest.mark.asyncio
        async def test_saves_feature_snapshot_when_repository_available(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_feature_repository: AsyncMock,
            sample_metadata: ModelMetadata,
        ) -> None:
            await service.train(raw_df)

            mock_feature_repository.save_snapshot.assert_awaited_once_with(
                sample_metadata.version,
                mock_feature_repository.save_snapshot.call_args[0][1],
            )

        @pytest.mark.asyncio
        async def test_skips_feature_save_when_repository_is_none(
            self,
            mock_model_registry: AsyncMock,
            mock_feature_engineering: MagicMock,
            mock_model_trainer: MagicMock,
            mock_settings,
            raw_df: pd.DataFrame,
        ) -> None:
            svc = TrainingService(
                registry=mock_model_registry,
                feature_engineering=mock_feature_engineering,
                model_trainer=mock_model_trainer,
                feature_repository=None,
                settings=mock_settings,
            )

            result = await svc.train(raw_df)

            assert isinstance(result, ModelMetadata)

        @pytest.mark.asyncio
        async def test_raises_training_error_when_feature_table_raises_value_error(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_feature_engineering: MagicMock,
        ) -> None:
            mock_feature_engineering.build_feature_table.side_effect = ValueError(
                "La columna 'line' es obligatoria."
            )

            with pytest.raises(TrainingError, match="obligatoria"):
                await service.train(raw_df)

        @pytest.mark.asyncio
        async def test_raises_training_error_when_dataset_is_empty(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_feature_engineering: MagicMock,
        ) -> None:
            mock_feature_engineering.build_feature_table.return_value = pd.DataFrame()

            with pytest.raises(TrainingError, match="No hay datos"):
                await service.train(raw_df)

        @pytest.mark.asyncio
        async def test_raises_training_error_when_evaluation_raises_value_error(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_model_trainer: MagicMock,
        ) -> None:
            mock_model_trainer.train_and_evaluate.side_effect = ValueError(
                "Se requieren al menos 6 meses."
            )

            with pytest.raises(TrainingError, match="6 meses"):
                await service.train(raw_df)

        @pytest.mark.asyncio
        async def test_metadata_includes_correct_fields(
            self,
            service: TrainingService,
            raw_df: pd.DataFrame,
            mock_model_registry: AsyncMock,
        ) -> None:
            await service.train(raw_df)

            call_args = mock_model_registry.save.call_args[0]
            metadata_dict = call_args[1]
            assert metadata_dict["target"] == "sales_count"
            assert "features" in metadata_dict
            assert "candidates" in metadata_dict
            assert "train_samples" in metadata_dict
            assert "test_samples" in metadata_dict
            assert "total_samples" in metadata_dict
            assert "residual_std" in metadata_dict["metrics"]
