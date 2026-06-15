"""Tests del servicio de monitoreo de drift."""

from __future__ import annotations

from unittest.mock import AsyncMock

import pytest

from app.application.services.monitoring_service import MonitoringService


@pytest.fixture
def mock_monitoring_repo() -> AsyncMock:
    repo = AsyncMock()
    repo.save_actual.return_value = None
    repo.load_drift_records.return_value = []
    return repo


@pytest.fixture
def service(mock_monitoring_repo: AsyncMock) -> MonitoringService:
    return MonitoringService(monitoring_repository=mock_monitoring_repo)


class TestRecordActual:
    """record_actual()"""

    @pytest.mark.asyncio
    async def test_shouldRecordActualAndDelegateToRepo(
        self, service: MonitoringService, mock_monitoring_repo: AsyncMock
    ) -> None:
        """Debe registrar demanda real delegando al repositorio."""
        await service.record_actual(
            model_version="20250101120000",
            segment={
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "XLE",
            },
            month="2025-01-01",
            actual_demand=15.0,
            predicted_demand=13.0,
        )
        mock_monitoring_repo.save_actual.assert_called_once()
        call_kwargs = mock_monitoring_repo.save_actual.call_args.kwargs
        assert call_kwargs["actual_demand"] == 15.0
        assert call_kwargs["predicted_demand"] == 13.0


class TestGetDriftReport:
    """get_drift_report()"""

    @pytest.mark.asyncio
    async def test_shouldReturnEmptyReportWhenNoRecords(
        self, service: MonitoringService
    ) -> None:
        """Debe retornar reporte vacío si no hay registros."""
        report = await service.get_drift_report("20250101120000")
        assert report["total_records"] == 0
        assert report["metrics"] == {}

    @pytest.mark.asyncio
    async def test_shouldComputeMetricsFromRecords(
        self, service: MonitoringService, mock_monitoring_repo: AsyncMock
    ) -> None:
        """Debe calcular MAE y MAPE de los registros."""
        mock_monitoring_repo.load_drift_records.return_value = [
            {
                "model_version": "v1",
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "XLE",
                "predicted_month": "2025-01-01",
                "predicted_demand": 13.0,
                "actual_demand": 15.0,
                "absolute_error": 2.0,
            },
            {
                "model_version": "v1",
                "vehicle_type": "CAR",
                "brand": "TOYOTA",
                "model": "COROLLA",
                "line": "XLE",
                "predicted_month": "2025-02-01",
                "predicted_demand": 10.0,
                "actual_demand": 12.0,
                "absolute_error": 2.0,
            },
        ]
        report = await service.get_drift_report("v1")
        assert report["total_records"] == 2
        assert report["metrics"]["mae"] == 2.0
        assert report["metrics"]["mape"] > 0
