"""Puerto para el repositorio de monitoreo de drift.

Define el contrato para registrar valores reales y compararlos
contra las predicciones almacenadas.
"""

from __future__ import annotations

from typing import Any, Dict, Protocol, runtime_checkable


@runtime_checkable
class MonitoringRepositoryPort(Protocol):
    """Contrato para persistir y consultar registros de drift."""

    async def save_actual(
        self,
        model_version: str,
        segment: Dict[str, Any],
        month: str,
        actual_demand: float,
        predicted_demand: float | None,
    ) -> None:
        """Registra un valor real de demanda para comparación con predicciones."""
        ...

    async def load_drift_records(
        self,
        model_version: str,
        segment: Dict[str, Any] | None = None,
    ) -> list[Dict[str, Any]]:
        """Carga registros de drift para un modelo y segmento opcional."""
        ...
