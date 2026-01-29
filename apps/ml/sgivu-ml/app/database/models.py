from __future__ import annotations

from datetime import date, datetime
from typing import Any

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    Float,
    Integer,
    JSON,
    LargeBinary,
    String,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base


class ModelArtifact(Base):
    """Modelo para almacenar artefactos de modelos de ML."""

    __tablename__ = "ml_model_artifacts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    model_name: Mapped[str] = mapped_column(String(128), index=True)
    version: Mapped[str] = mapped_column(String(32), unique=True, index=True)
    model_metadata: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    artifact: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )


class TrainingFeature(Base):
    """Modelo para almacenar características de entrenamiento de ML."""

    __tablename__ = "ml_training_features"
    __table_args__ = (
        UniqueConstraint(
            "model_version",
            "vehicle_type",
            "brand",
            "model",
            "line",
            "event_month",
            name="uq_ml_training_feature",
        ),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    model_version: Mapped[str] = mapped_column(String(32), index=True)
    event_month: Mapped[date] = mapped_column(Date, index=True)
    vehicle_type: Mapped[str] = mapped_column(String(32), index=True)
    brand: Mapped[str] = mapped_column(String(120), index=True)
    model: Mapped[str] = mapped_column(String(120), index=True)
    line: Mapped[str] = mapped_column(String(120), index=True)
    sales_count: Mapped[float] = mapped_column(Float)
    purchases_count: Mapped[float] = mapped_column(Float)
    avg_margin: Mapped[float] = mapped_column(Float)
    avg_sale_price: Mapped[float] = mapped_column(Float)
    avg_purchase_price: Mapped[float] = mapped_column(Float)
    avg_days_inventory: Mapped[float] = mapped_column(Float)
    inventory_rotation: Mapped[float] = mapped_column(Float)
    lag_1: Mapped[float] = mapped_column(Float)
    lag_3: Mapped[float] = mapped_column(Float)
    lag_6: Mapped[float] = mapped_column(Float)
    rolling_mean_3: Mapped[float] = mapped_column(Float)
    rolling_mean_6: Mapped[float] = mapped_column(Float)
    month: Mapped[int] = mapped_column(Integer)
    year: Mapped[int] = mapped_column(Integer)
    month_sin: Mapped[float] = mapped_column(Float)
    month_cos: Mapped[float] = mapped_column(Float)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )


class PredictionRecord(Base):
    """Modelo para almacenar registros de predicciones de ML."""

    __tablename__ = "ml_predictions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    model_version: Mapped[str] = mapped_column(String(32), index=True)
    request_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    response_payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    vehicle_type: Mapped[str | None] = mapped_column(String(32), nullable=True)
    brand: Mapped[str | None] = mapped_column(String(120), nullable=True)
    model: Mapped[str | None] = mapped_column(String(120), nullable=True)
    line: Mapped[str | None] = mapped_column(String(120), nullable=True)
    horizon_months: Mapped[int | None] = mapped_column(Integer, nullable=True)
    confidence: Mapped[float | None] = mapped_column(Float, nullable=True)
    with_history: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
