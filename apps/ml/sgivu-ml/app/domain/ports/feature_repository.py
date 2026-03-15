"""Puerto para el repositorio de features de entrenamiento.

Desacopla la lógica de negocio del medio de persistencia concreto
(PostgreSQL, archivos, etc.).
"""

from __future__ import annotations

from typing import Any, Dict, Protocol, runtime_checkable

import pandas as pd


@runtime_checkable
class FeatureRepositoryPort(Protocol):
    """Contrato para persistir y recuperar snapshots de features."""

    async def save_snapshot(self, model_version: str, feature_df: pd.DataFrame) -> None:
        """Guarda un snapshot completo de features para una versión de modelo."""
        ...

    async def load_snapshot(self, model_version: str) -> pd.DataFrame:
        """Carga el snapshot completo de features de una versión."""
        ...

    async def load_segment_history(
        self, model_version: str, filters: Dict[str, Any]
    ) -> pd.DataFrame:
        """Carga el historial de features filtrado por segmento de vehículo."""
        ...
