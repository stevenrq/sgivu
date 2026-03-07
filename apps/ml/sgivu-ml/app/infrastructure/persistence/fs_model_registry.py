"""Registro de modelos de ML en sistema de archivos.

Implementa ``ModelRegistryPort`` usando el filesystem local.
Las operaciones de I/O se ejecutan con ``asyncio.to_thread``
para evitar bloquear el event loop.
"""

from __future__ import annotations

import asyncio
import json
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib

from app.domain.entities import ModelMetadata
from app.infrastructure.config import Settings

logger = logging.getLogger(__name__)


class FileSystemModelRegistry:
    """Registro de modelos basado en el sistema de archivos local."""

    def __init__(self, settings: Settings) -> None:
        self._model_dir: Path = settings.model_path()
        self._model_name: str = settings.model_name
        self._latest_metadata_path = self._model_dir / "latest.json"

    def _artifact_path(self, version: str) -> Path:
        return self._model_dir / f"{self._model_name}_{version}.joblib"

    async def save(self, model: Any, metadata: dict[str, Any]) -> ModelMetadata:
        """Serializa y persiste el modelo en el filesystem."""
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        merged = {**metadata, "version": timestamp}
        artifact_path = self._artifact_path(timestamp)

        def _write() -> None:
            joblib.dump({"model": model, "metadata": merged}, artifact_path)
            self._latest_metadata_path.write_text(json.dumps(merged, indent=2))

        await asyncio.to_thread(_write)
        logger.info("Model saved to filesystem with version %s", timestamp)
        return ModelMetadata(**merged)

    async def load_latest(self) -> tuple[Any, ModelMetadata]:
        """Carga el último modelo entrenado desde el filesystem."""

        def _read() -> tuple[Any, dict[str, Any]]:
            if not self._latest_metadata_path.exists():
                raise FileNotFoundError("No se encontro modelo entrenado.")
            raw = json.loads(self._latest_metadata_path.read_text())
            artifact_path = self._artifact_path(raw["version"])
            artifact = joblib.load(artifact_path)
            model = artifact.get("model", artifact)
            model_metadata = artifact.get("metadata", raw)
            return model, model_metadata

        model, raw_metadata = await asyncio.to_thread(_read)
        return model, ModelMetadata(**raw_metadata)

    async def latest_metadata(self) -> ModelMetadata | None:
        """Obtiene la metadata del último modelo sin cargar el artefacto."""

        def _read() -> dict[str, Any] | None:
            if not self._latest_metadata_path.exists():
                return None
            return json.loads(self._latest_metadata_path.read_text())

        raw = await asyncio.to_thread(_read)
        if raw is None:
            return None
        return ModelMetadata(**raw)
