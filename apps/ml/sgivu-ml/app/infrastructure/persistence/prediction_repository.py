"""Implementación async del repositorio de predicciones.

Implementa ``PredictionRepositoryPort`` usando SQLAlchemy async + asyncpg.
"""

from __future__ import annotations

from typing import Any, Dict

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.infrastructure.persistence.models import PredictionRecord


class PredictionRepository:
    """Persistencia async de registros de predicciones."""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def save_prediction(
        self,
        model_version: str,
        request_payload: Dict[str, Any],
        response_payload: Dict[str, Any],
        segment: Dict[str, Any] | None = None,
        horizon: int | None = None,
        confidence: float | None = None,
        with_history: bool = False,
    ) -> None:
        """Guarda la solicitud y su respuesta como registro de auditoría."""
        segment = segment or {}
        record = PredictionRecord(
            model_version=model_version,
            request_payload=request_payload,
            response_payload=response_payload,
            vehicle_type=segment.get("vehicle_type"),
            brand=segment.get("brand"),
            model=segment.get("model"),
            line=segment.get("line"),
            horizon_months=horizon,
            confidence=confidence,
            with_history=with_history,
        )
        async with self._session_factory() as session:
            async with session.begin():
                session.add(record)
