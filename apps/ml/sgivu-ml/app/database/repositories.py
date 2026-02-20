from __future__ import annotations

import logging
from typing import Any, Dict

import pandas as pd
from sqlalchemy import delete, select

from app.core.config import Settings, get_settings
from app.database.database import database_enabled, session_scope
from app.database.models import PredictionRecord, TrainingFeature

logger = logging.getLogger(__name__)


def _safe_float(value: Any) -> float:
    if value is None or pd.isna(value):
        return 0.0
    return float(value)


class FeatureStore:
    """Persistencia de snapshots de features de entrenamiento."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.enabled = database_enabled(self.settings)

    def save_snapshot(self, model_version: str, feature_df: pd.DataFrame) -> None:
        """Guarda el snapshot de features en la base de datos."""
        if not self.enabled or feature_df.empty:
            return

        payloads: list[Dict[str, Any]] = []
        for row in feature_df.itertuples(index=False):
            event_month = getattr(row, "event_month", None)
            payloads.append(
                {
                    "model_version": model_version,
                    "event_month": (
                        event_month.date()
                        if event_month is not None and hasattr(event_month, "date")
                        else event_month
                    ),
                    "vehicle_type": getattr(row, "vehicle_type", None),
                    "brand": getattr(row, "brand", None),
                    "model": getattr(row, "model", None),
                    "line": getattr(row, "line", None),
                    "sales_count": _safe_float(getattr(row, "sales_count", None)),
                    "purchases_count": _safe_float(
                        getattr(row, "purchases_count", None)
                    ),
                    "avg_margin": _safe_float(getattr(row, "avg_margin", None)),
                    "avg_sale_price": _safe_float(getattr(row, "avg_sale_price", None)),
                    "avg_purchase_price": _safe_float(
                        getattr(row, "avg_purchase_price", None)
                    ),
                    "avg_days_inventory": _safe_float(
                        getattr(row, "avg_days_inventory", None)
                    ),
                    "inventory_rotation": _safe_float(
                        getattr(row, "inventory_rotation", None)
                    ),
                    "lag_1": _safe_float(getattr(row, "lag_1", None)),
                    "lag_3": _safe_float(getattr(row, "lag_3", None)),
                    "lag_6": _safe_float(getattr(row, "lag_6", None)),
                    "rolling_mean_3": _safe_float(getattr(row, "rolling_mean_3", None)),
                    "rolling_mean_6": _safe_float(getattr(row, "rolling_mean_6", None)),
                    "month": int(getattr(row, "month", 0) or 0),
                    "year": int(getattr(row, "year", 0) or 0),
                    "month_sin": _safe_float(getattr(row, "month_sin", None)),
                    "month_cos": _safe_float(getattr(row, "month_cos", None)),
                }
            )

        with session_scope(self.settings) as session:
            session.execute(
                delete(TrainingFeature).where(
                    TrainingFeature.model_version == model_version
                )
            )
            if payloads:
                session.bulk_insert_mappings(TrainingFeature.__mapper__, payloads)

        logger.info(
            "Feature snapshot saved: %s rows for version %s",
            len(payloads),
            model_version,
        )

    def load_snapshot(self, model_version: str) -> pd.DataFrame:
        """Carga el snapshot completo de features desde la base de datos."""
        if not self.enabled:
            return pd.DataFrame()

        with session_scope(self.settings) as session:
            rows = (
                session.execute(
                    select(TrainingFeature)
                    .where(TrainingFeature.model_version == model_version)
                    .order_by(TrainingFeature.event_month)
                )
                .scalars()
                .all()
            )

        if not rows:
            return pd.DataFrame()

        data = [
            {
                "event_month": pd.to_datetime(row.event_month),
                "vehicle_type": row.vehicle_type,
                "brand": row.brand,
                "model": row.model,
                "line": row.line,
                "sales_count": row.sales_count,
                "purchases_count": row.purchases_count,
                "avg_margin": row.avg_margin,
                "avg_sale_price": row.avg_sale_price,
                "avg_purchase_price": row.avg_purchase_price,
                "avg_days_inventory": row.avg_days_inventory,
                "inventory_rotation": row.inventory_rotation,
                "lag_1": row.lag_1,
                "lag_3": row.lag_3,
                "lag_6": row.lag_6,
                "rolling_mean_3": row.rolling_mean_3,
                "rolling_mean_6": row.rolling_mean_6,
                "month": row.month,
                "year": row.year,
                "month_sin": row.month_sin,
                "month_cos": row.month_cos,
            }
            for row in rows
        ]
        return pd.DataFrame(data)

    def load_segment_history(
        self, model_version: str, filters: Dict[str, Any]
    ) -> pd.DataFrame:
        if not self.enabled:
            return pd.DataFrame()

        with session_scope(self.settings) as session:
            rows = (
                session.execute(
                    select(TrainingFeature)
                    .where(TrainingFeature.model_version == model_version)
                    .where(TrainingFeature.vehicle_type == filters.get("vehicle_type"))
                    .where(TrainingFeature.brand == filters.get("brand"))
                    .where(TrainingFeature.model == filters.get("model"))
                    .where(TrainingFeature.line == filters.get("line"))
                    .order_by(TrainingFeature.event_month)
                )
                .scalars()
                .all()
            )

        if not rows:
            return pd.DataFrame()

        data = [
            {
                "event_month": pd.to_datetime(row.event_month),
                "vehicle_type": row.vehicle_type,
                "brand": row.brand,
                "model": row.model,
                "line": row.line,
                "sales_count": row.sales_count,
                "purchases_count": row.purchases_count,
                "avg_margin": row.avg_margin,
                "avg_sale_price": row.avg_sale_price,
                "avg_purchase_price": row.avg_purchase_price,
                "avg_days_inventory": row.avg_days_inventory,
                "inventory_rotation": row.inventory_rotation,
                "lag_1": row.lag_1,
                "lag_3": row.lag_3,
                "lag_6": row.lag_6,
                "rolling_mean_3": row.rolling_mean_3,
                "rolling_mean_6": row.rolling_mean_6,
                "month": row.month,
                "year": row.year,
                "month_sin": row.month_sin,
                "month_cos": row.month_cos,
            }
            for row in rows
        ]
        return pd.DataFrame(data)


class PredictionStore:
    """Persistencia de registros de predicciones de ML."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.enabled = database_enabled(self.settings)

    def save_prediction(
        self,
        model_version: str,
        request_payload: Dict[str, Any],
        response_payload: Dict[str, Any],
        segment: Dict[str, Any] | None = None,
        horizon: int | None = None,
        confidence: float | None = None,
        with_history: bool = False,
    ) -> None:
        """Guarda la solicitud y su respuesta en la base de datos."""
        if not self.enabled:
            return

        segment = segment or {}
        record = PredictionRecord(
            model_version=model_version,
            request_payload=request_payload,
            response_payload=response_payload,
            vehicle_type=segment.get("vehicle_type"),
            brand=segment.get("brand"),
            model=segment.get("model"),
            line=segment.get("line"),
            horizon_months=horizon,
            confidence=confidence,
            with_history=with_history,
        )

        with session_scope(self.settings) as session:
            session.add(record)
