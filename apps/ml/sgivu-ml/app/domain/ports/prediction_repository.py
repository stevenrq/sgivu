"""Puerto para el repositorio de predicciones.

Permite auditar las predicciones realizadas sin acoplar la lógica
de negocio al motor de persistencia.
"""

from __future__ import annotations

from typing import Any, Dict, Protocol, runtime_checkable


@runtime_checkable
class PredictionRepositoryPort(Protocol):
    """Contrato para persistir registros de predicciones."""

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
        ...
