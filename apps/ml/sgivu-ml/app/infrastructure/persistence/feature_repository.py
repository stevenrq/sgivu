"""Implementación async del repositorio de features de entrenamiento.

Implementa ``FeatureRepositoryPort`` usando SQLAlchemy async + asyncpg.
"""

from __future__ import annotations

import logging
from typing import Any, Dict

import pandas as pd
from sqlalchemy import delete, insert, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.infrastructure.persistence.models import TrainingFeature

logger = logging.getLogger(__name__)


def _safe_float(value: Any) -> float:
    """Convierte un valor a float, retornando 0.0 si es None o NaN."""
    if value is None or pd.isna(value):
        return 0.0
    return float(value)


class FeatureRepository:
    """Persistencia async de snapshots de features de entrenamiento."""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def save_snapshot(self, model_version: str, feature_df: pd.DataFrame) -> None:
        """Guarda el snapshot de features en la base de datos."""
        if feature_df.empty:
            return

        payloads = self._build_payloads(model_version, feature_df)

        async with self._session_factory() as session:
            async with session.begin():
                await session.execute(
                    delete(TrainingFeature).where(
                        TrainingFeature.model_version == model_version
                    )
                )
                if payloads:
                    await session.execute(insert(TrainingFeature), payloads)

        logger.info(
            "Feature snapshot saved: %s rows for version %s",
            len(payloads),
            model_version,
        )

    async def load_snapshot(self, model_version: str) -> pd.DataFrame:
        """Carga el snapshot completo de features desde la base de datos."""
        async with self._session_factory() as session:
            result = await session.execute(
                select(TrainingFeature)
                .where(TrainingFeature.model_version == model_version)
                .order_by(TrainingFeature.event_month)
            )
            rows = result.scalars().all()

        if not rows:
            return pd.DataFrame()
        return self._rows_to_dataframe(rows)

    async def load_segment_history(
        self, model_version: str, filters: Dict[str, Any]
    ) -> pd.DataFrame:
        """Carga historial de features filtrado por segmento de vehículo."""
        async with self._session_factory() as session:
            result = await session.execute(
                select(TrainingFeature)
                .where(TrainingFeature.model_version == model_version)
                .where(TrainingFeature.vehicle_type == filters.get("vehicle_type"))
                .where(TrainingFeature.brand == filters.get("brand"))
                .where(TrainingFeature.model == filters.get("model"))
                .where(TrainingFeature.line == filters.get("line"))
                .order_by(TrainingFeature.event_month)
            )
            rows = result.scalars().all()

        if not rows:
            return pd.DataFrame()
        return self._rows_to_dataframe(rows)

    @staticmethod
    def _build_payloads(
        model_version: str, feature_df: pd.DataFrame
    ) -> list[Dict[str, Any]]:
        """Transforma el DataFrame de features a una lista de dicts insertables."""
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
        return payloads

    @staticmethod
    def _rows_to_dataframe(rows: list[Any]) -> pd.DataFrame:
        """Convierte filas ORM a un DataFrame de pandas."""
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
