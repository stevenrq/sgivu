from __future__ import annotations

from datetime import date
from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class PredictionRequest(BaseModel):
    """Petición de pronóstico de demanda por segmento."""

    vehicle_type: str = Field(..., description="Tipo de vehiculo (CAR/MOTORCYCLE)")
    brand: str
    model: str
    line: str = Field(
        ..., description="Línea/versión (obligatoria)", min_length=1, strip_whitespace=True
    )
    horizon_months: int = Field(6, gt=0, le=24)
    confidence: float = Field(0.95, ge=0.5, le=0.99)


class MonthlyPrediction(BaseModel):
    """Valor de demanda estimada para un mes futuro."""

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
    """Valor histórico de ventas agregadas por mes."""

    month: str
    sales_count: float


class PredictionWithHistoryResponse(BaseModel):
    """Pronóstico junto con historial para graficar en frontend."""

    predictions: List[MonthlyPrediction]
    history: List[HistoricalPoint]
    segment: Dict[str, str]
    model_version: str
    trained_at: Optional[str] = None
    metrics: Optional[Dict[str, float]]


class RetrainRequest(BaseModel):
    """Parametros opcionales para acotar el reentrenamiento."""

    start_date: Optional[date] = Field(None, description="Fecha inicial (opcional)")
    end_date: Optional[date] = Field(None, description="Fecha final (opcional)")


class RetrainResponse(BaseModel):
    """Metadata del modelo entrenado tras un reentrenamiento."""

    version: str
    metrics: Dict[str, float]
    trained_at: str
    samples: Dict[str, int]
