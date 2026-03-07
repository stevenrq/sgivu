"""Entidades de dominio del microservicio de ML.

Son value objects puros modelados con Pydantic V2. No dependen de ningún
framework HTTP ni de infraestructura.
"""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict


class VehicleSegment(BaseModel):
    """Segmento de vehículo identificado por tipo, marca, modelo y línea."""

    vehicle_type: str
    brand: str
    model: str
    line: str


class ForecastPoint(BaseModel):
    """Punto de pronóstico con intervalos de confianza."""

    month: str
    demand: float
    lower_ci: float
    upper_ci: float


class HistoricalPoint(BaseModel):
    """Punto histórico de ventas."""

    month: str
    sales_count: float


class ModelMetadata(BaseModel):
    """Metadata completa de un modelo entrenado."""

    model_config = ConfigDict(extra="allow")

    version: str
    trained_at: str | None = None
    target: str | None = None
    features: list[str] | None = None
    metrics: dict[str, float] | None = None
    candidates: list[dict[str, Any]] | None = None
    train_samples: int | None = None
    test_samples: int | None = None
    total_samples: int | None = None


class PredictionResult(BaseModel):
    """Resultado de una predicción de demanda."""

    predictions: list[ForecastPoint]
    model_version: str
    metrics: dict[str, float] | None = None


class PredictionWithHistoryResult(BaseModel):
    """Resultado de una predicción con historial para gráficos."""

    predictions: list[ForecastPoint]
    history: list[HistoricalPoint]
    segment: VehicleSegment
    model_version: str
    trained_at: str | None = None
    metrics: dict[str, float] | None = None
