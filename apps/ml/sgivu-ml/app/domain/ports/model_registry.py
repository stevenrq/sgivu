"""Puerto para el registro de modelos de ML.

Define el contrato que deben cumplir las implementaciones concretas
(filesystem, base de datos, S3, MLflow, etc.).
"""

from __future__ import annotations

from typing import Any, Protocol, runtime_checkable

from app.domain.entities import ModelMetadata


@runtime_checkable
class ModelRegistryPort(Protocol):
    """Contrato para persistir y recuperar modelos entrenados."""

    async def save(self, model: Any, metadata: dict[str, Any]) -> ModelMetadata:
        """Persiste un modelo y su metadata, asignando una nueva versión."""
        ...

    async def load_latest(self) -> tuple[Any, ModelMetadata]:
        """Carga el último modelo entrenado junto con su metadata."""
        ...

    async def latest_metadata(self) -> ModelMetadata | None:
        """Obtiene la metadata del último modelo sin cargar el artefacto."""
        ...
