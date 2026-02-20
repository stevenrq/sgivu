from __future__ import annotations

from datetime import date
from typing import Dict, List, Optional

from pydantic import BaseModel, Field, field_validator


class PredictionRequest(BaseModel):
    """Solicitud de predicción de demanda mensual."""

    vehicle_type: str = Field(..., description="Tipo de vehiculo (CAR/MOTORCYCLE)")
    brand: str
    model: str
    line: str = Field(
        ...,
        description="Línea/versión (obligatoria)",
        min_length=1,
    )

    @field_validator("line", mode="before")
    def _strip_line(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    horizon_months: int = Field(6, gt=0, le=24)
    confidence: float = Field(0.95, ge=0.5, le=0.99)


class MonthlyPrediction(BaseModel):
    """Predicción de demanda para un mes específico."""

    month: str
    demand: float
    lower_ci: float
    upper_ci: float


class PredictionResponse(BaseModel):
    """Respuesta con la serie pronosticada y metadata de modelo."""

    predictions: List[MonthlyPrediction]
    model_version: str
    metrics: Optional[Dict[str, float]]


class HistoricalPoint(BaseModel):
    """Punto histórico de ventas."""

    month: str
    sales_count: float


class PredictionWithHistoryResponse(BaseModel):
    """Respuesta con predicción y datos históricos."""

    predictions: List[MonthlyPrediction]
    history: List[HistoricalPoint]
    segment: Dict[str, str]
    model_version: str
    trained_at: Optional[str] = None
    metrics: Optional[Dict[str, float]]


class RetrainRequest(BaseModel):
    """Solicitud para reentrenar el modelo."""

    start_date: Optional[date] = Field(None, description="Fecha inicial (opcional)")
    end_date: Optional[date] = Field(None, description="Fecha final (opcional)")


class RetrainResponse(BaseModel):
    """Respuesta con detalles del reentrenamiento."""

    version: str
    metrics: Dict[str, float]
    trained_at: str
    samples: Dict[str, int]
