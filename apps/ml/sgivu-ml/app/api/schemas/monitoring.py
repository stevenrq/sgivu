"""Schemas Pydantic para los endpoints de monitoreo de drift."""

from __future__ import annotations

from typing import Any, Dict

from pydantic import BaseModel, Field


class ActualDemandRequest(BaseModel):
    """Solicitud para registrar demanda real."""

    model_version: str = Field(..., description="Versión del modelo de referencia")
    vehicle_type: str = Field(..., description="Tipo de vehículo (CAR/MOTORCYCLE)")
    brand: str
    model: str
    line: str = Field(..., min_length=1)
    month: str = Field(
        ...,
        description="Mes en formato ISO (YYYY-MM-DD)",
        pattern=r"^\d{4}-\d{2}-\d{2}$",
    )
    actual_demand: float = Field(..., ge=0, description="Demanda real del mes")
    predicted_demand: float | None = Field(
        None, description="Predicción original (opcional)"
    )


class ActualDemandResponse(BaseModel):
    """Respuesta tras registrar demanda real."""

    status: str = "recorded"
    model_version: str
    month: str


class DriftReportResponse(BaseModel):
    """Reporte de drift del modelo."""

    model_version: str
    total_records: int
    records_with_prediction: int | None = None
    metrics: Dict[str, float]
    records: list[Dict[str, Any]]
