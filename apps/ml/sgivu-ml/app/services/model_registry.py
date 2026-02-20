from __future__ import annotations

import io
import json
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Tuple

import joblib
from sqlalchemy import select

from app.database.database import database_enabled, session_scope
from app.database.models import ModelArtifact
from app.core.config import Settings, get_settings

logger = logging.getLogger(__name__)


class ModelRegistry:
    """Registro de modelos basado en el sistema de archivos local."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.model_dir: Path = self.settings.model_path()
        self.latest_metadata_path = self.model_dir / "latest.json"

    def _artifact_path(self, version: str) -> Path:
        return self.model_dir / f"{self.settings.model_name}_{version}.joblib"

    def save(self, model: Any, metadata: Dict[str, Any]) -> Dict[str, Any]:
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        merged_metadata = {**metadata, "version": timestamp}
        artifact_path = self._artifact_path(timestamp)

        joblib.dump({"model": model, "metadata": merged_metadata}, artifact_path)
        self.latest_metadata_path.write_text(json.dumps(merged_metadata, indent=2))
        return merged_metadata

    def load_latest(self) -> Tuple[Any, Dict[str, Any]]:
        if not self.latest_metadata_path.exists():
            raise FileNotFoundError("No se encontro modelo entrenado.")

        metadata = json.loads(self.latest_metadata_path.read_text())
        artifact_path = self._artifact_path(metadata["version"])
        artifact = joblib.load(artifact_path)
        model = artifact.get("model", artifact)
        model_metadata = artifact.get("metadata", metadata)
        return model, model_metadata

    def latest_metadata(self) -> Dict[str, Any] | None:
        if not self.latest_metadata_path.exists():
            return None
        return json.loads(self.latest_metadata_path.read_text())


class DatabaseModelRegistry:
    """Registro de modelos usando una base de datos SQL."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.enabled = database_enabled(self.settings)

    def save(self, model: Any, metadata: Dict[str, Any]) -> Dict[str, Any]:
        if not self.enabled:
            raise RuntimeError("DATABASE_URL no configurada para guardar el modelo.")

        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        merged_metadata = {**metadata, "version": timestamp}
        buffer = io.BytesIO()
        joblib.dump({"model": model, "metadata": merged_metadata}, buffer)

        record = ModelArtifact(
            model_name=self.settings.model_name,
            version=timestamp,
            model_metadata=merged_metadata,
            artifact=buffer.getvalue(),
        )

        with session_scope(self.settings) as session:
            session.add(record)

        logger.info("Model saved in DB with version %s", timestamp)
        return merged_metadata

    def load_latest(self) -> Tuple[Any, Dict[str, Any]]:
        if not self.enabled:
            raise FileNotFoundError("No hay base de datos configurada.")

        with session_scope(self.settings) as session:
            record = (
                session.execute(
                    select(ModelArtifact)
                    .where(ModelArtifact.model_name == self.settings.model_name)
                    .order_by(ModelArtifact.created_at.desc())
                    .limit(1)
                )
                .scalars()
                .first()
            )

        if not record:
            raise FileNotFoundError("No se encontro modelo entrenado.")

        artifact = joblib.load(io.BytesIO(record.artifact))
        model = artifact.get("model", artifact)
        metadata = artifact.get("metadata", record.model_metadata)
        return model, metadata

    def latest_metadata(self) -> Dict[str, Any] | None:
        if not self.enabled:
            return None

        with session_scope(self.settings) as session:
            record = (
                session.execute(
                    select(ModelArtifact)
                    .where(ModelArtifact.model_name == self.settings.model_name)
                    .order_by(ModelArtifact.created_at.desc())
                    .limit(1)
                )
                .scalars()
                .first()
            )

        if not record:
            return None
        return record.model_metadata
