"""Implementación async del repositorio de monitoreo de drift."""

from __future__ import annotations

import logging
from typing import Any, Dict

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.infrastructure.persistence.models import DriftRecord

logger = logging.getLogger(__name__)


class MonitoringRepository:
    """Persistencia async de registros de drift predicción vs real."""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def save_actual(
        self,
        model_version: str,
        segment: Dict[str, Any],
        month: str,
        actual_demand: float,
        predicted_demand: float | None,
    ) -> None:
        """Registra un valor real y calcula el error absoluto."""
        absolute_error = (
            abs(actual_demand - predicted_demand)
            if predicted_demand is not None
            else None
        )
        record = DriftRecord(
            model_version=model_version,
            vehicle_type=segment.get("vehicle_type"),
            brand=segment.get("brand"),
            model=segment.get("model"),
            line=segment.get("line"),
            predicted_month=month,
            predicted_demand=predicted_demand,
            actual_demand=actual_demand,
            absolute_error=absolute_error,
        )
        async with self._session_factory() as session:
            async with session.begin():
                session.add(record)

    async def load_drift_records(
        self,
        model_version: str,
        segment: Dict[str, Any] | None = None,
    ) -> list[Dict[str, Any]]:
        """Carga registros de drift filtrados por modelo y segmento."""
        async with self._session_factory() as session:
            stmt = select(DriftRecord).where(DriftRecord.model_version == model_version)
            if segment:
                if segment.get("vehicle_type"):
                    stmt = stmt.where(
                        DriftRecord.vehicle_type == segment["vehicle_type"]
                    )
                if segment.get("brand"):
                    stmt = stmt.where(DriftRecord.brand == segment["brand"])
                if segment.get("model"):
                    stmt = stmt.where(DriftRecord.model == segment["model"])
                if segment.get("line"):
                    stmt = stmt.where(DriftRecord.line == segment["line"])

            result = await session.execute(stmt.order_by(DriftRecord.predicted_month))
            rows = result.scalars().all()

        return [
            {
                "model_version": r.model_version,
                "vehicle_type": r.vehicle_type,
                "brand": r.brand,
                "model": r.model,
                "line": r.line,
                "predicted_month": r.predicted_month,
                "predicted_demand": r.predicted_demand,
                "actual_demand": r.actual_demand,
                "absolute_error": r.absolute_error,
            }
            for r in rows
        ]
