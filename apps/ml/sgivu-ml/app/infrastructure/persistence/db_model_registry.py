"""Registro de modelos de ML en base de datos.

Implementa ``ModelRegistryPort`` almacenando artefactos serializados
con joblib como BYTEA en PostgreSQL.
"""

from __future__ import annotations

import io
import logging
from datetime import datetime, timezone
from typing import Any

import joblib
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.domain.entities import ModelMetadata
from app.infrastructure.persistence.models import ModelArtifact

logger = logging.getLogger(__name__)


class DatabaseModelRegistry:
    """Registro de modelos usando una base de datos SQL (async)."""

    def __init__(
        self,
        session_factory: async_sessionmaker[AsyncSession],
        model_name: str,
    ) -> None:
        self._session_factory = session_factory
        self._model_name = model_name

    async def save(self, model: Any, metadata: dict[str, Any]) -> ModelMetadata:
        """Serializa y persiste el modelo en la base de datos."""
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        merged = {**metadata, "version": timestamp}

        buffer = io.BytesIO()
        joblib.dump({"model": model, "metadata": merged}, buffer)

        record = ModelArtifact(
            model_name=self._model_name,
            version=timestamp,
            model_metadata=merged,
            artifact=buffer.getvalue(),
        )

        async with self._session_factory() as session:
            async with session.begin():
                session.add(record)

        logger.info("Model saved in DB with version %s", timestamp)
        return ModelMetadata(**merged)

    async def load_latest(self) -> tuple[Any, ModelMetadata]:
        """Carga el último modelo entrenado desde la base de datos."""
        async with self._session_factory() as session:
            result = await session.execute(
                select(ModelArtifact)
                .where(ModelArtifact.model_name == self._model_name)
                .order_by(ModelArtifact.created_at.desc())
                .limit(1)
            )
            record = result.scalars().first()

        if not record:
            raise FileNotFoundError("No se encontro modelo entrenado.")

        artifact = joblib.load(io.BytesIO(record.artifact))
        model = artifact.get("model", artifact)
        raw_metadata = artifact.get("metadata", record.model_metadata)
        return model, ModelMetadata(**raw_metadata)

    async def latest_metadata(self) -> ModelMetadata | None:
        """Obtiene la metadata del último modelo sin cargar el artefacto."""
        async with self._session_factory() as session:
            result = await session.execute(
                select(ModelArtifact)
                .where(ModelArtifact.model_name == self._model_name)
                .order_by(ModelArtifact.created_at.desc())
                .limit(1)
            )
            record = result.scalars().first()

        if not record:
            return None
        return ModelMetadata(**record.model_metadata)
