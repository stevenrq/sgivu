"""Ingeniería de features para modelos de demanda.

Responsabilidad única: transformar transacciones brutas en features
numéricas y categóricas aptas para modelos de ML.
"""

from __future__ import annotations

from typing import Any, Dict

import numpy as np
import pandas as pd

from app.infrastructure.config import Settings
from app.infrastructure.ml.normalization import (
    canonicalize_brand_model,
    canonicalize_label,
)


class FeatureEngineering:
    """Construye tablas de features a partir de transacciones brutas."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.category_cols: list[str] = ["vehicle_type", "brand", "model", "line"]
        self.optional_category_cols: list[str] = []
        self.numeric_cols: list[str] = [
            "purchases_count",
            "avg_margin",
            "avg_sale_price",
            "avg_purchase_price",
            "avg_days_inventory",
            "inventory_rotation",
            "lag_1",
            "lag_3",
            "lag_6",
            "rolling_mean_3",
            "rolling_mean_6",
            "month",
            "year",
            "month_sin",
            "month_cos",
        ]

    def build_feature_table(self, df: pd.DataFrame) -> pd.DataFrame:
        """Transforma transacciones brutas en una tabla de features mensual.

        Raises:
            ValueError: si falta la columna ``line`` o tiene registros vacíos.
        """
        if df.empty:
            return df

        work_df = df.copy()
        if "line" not in work_df.columns:
            raise ValueError(
                "La columna 'line' es obligatoria para entrenar el modelo."
            )

        for col in self.category_cols + self.optional_category_cols:
            if col in work_df:
                work_df[col] = work_df[col].fillna("UNKNOWN").apply(canonicalize_label)

        if "line" in work_df:
            missing_lines = (work_df["line"] == "") | (work_df["line"] == "UNKNOWN")
            if missing_lines.any():
                raise ValueError(
                    "Todos los registros deben tener línea de vehículo (no vacía)."
                )

        if "brand" in work_df and "model" in work_df:
            pairs = work_df[["brand", "model"]].apply(
                lambda row: canonicalize_brand_model(row["brand"], row["model"]),
                axis=1,
                result_type="expand",
            )
            work_df[["brand", "model"]] = pairs.values

        work_df["event_date"] = pd.to_datetime(
            work_df["updated_at"].fillna(work_df["created_at"]), utc=True
        ).dt.tz_localize(None)
        work_df["event_month"] = (
            pd.DatetimeIndex(work_df["event_date"]).to_period("M").to_timestamp()
        )

        work_df["is_sale"] = work_df["contract_type"] == "SALE"
        work_df["is_purchase"] = work_df["contract_type"] == "PURCHASE"
        work_df["margin"] = work_df["sale_price"] - work_df["purchase_price"]

        purchase_dates = (
            work_df[work_df["is_purchase"]]
            .sort_values("event_date")
            .drop_duplicates("vehicle_id", keep="first")
            .set_index("vehicle_id")["event_date"]
        )
        work_df["purchase_date"] = work_df["vehicle_id"].map(purchase_dates)
        work_df["days_in_inventory"] = np.where(
            work_df["is_sale"] & work_df["purchase_date"].notna(),
            (work_df["event_date"].to_numpy() - work_df["purchase_date"].to_numpy())
            / np.timedelta64(1, "D"),
            np.nan,
        )

        group_cols = self.category_cols + ["event_month"]
        monthly = (
            work_df.groupby(group_cols)
            .agg(
                sales_count=("is_sale", "sum"),
                purchases_count=("is_purchase", "sum"),
                avg_sale_price=("sale_price", "mean"),
                avg_purchase_price=("purchase_price", "mean"),
                avg_margin=("margin", "mean"),
                avg_days_inventory=("days_in_inventory", "mean"),
            )
            .reset_index()
        )

        monthly["inventory_rotation"] = monthly["sales_count"] / monthly[
            "purchases_count"
        ].clip(lower=1)

        monthly = self._add_time_features(monthly)
        grouped_frames = [
            self._add_lags(g.copy())
            for _, g in monthly.groupby(self.category_cols, sort=False)
        ]
        monthly = (
            pd.concat(grouped_frames, ignore_index=True)
            if grouped_frames
            else pd.DataFrame()
        )

        for col in ["lag_1", "lag_3", "lag_6", "rolling_mean_3", "rolling_mean_6"]:
            if col not in monthly:
                monthly[col] = np.nan

        monthly = monthly.sort_values("event_month")
        monthly[self.numeric_cols] = monthly[self.numeric_cols].fillna(0)
        return monthly

    def build_future_row(
        self, history: pd.DataFrame, target_month: pd.Timestamp
    ) -> pd.DataFrame:
        """Construye una fila de features para un mes futuro basándose en el historial.

        Raises:
            ValueError: si el historial está vacío.
        """
        if history.empty:
            raise ValueError("No hay historial para calcular predicciones.")

        history = history.sort_values("event_month")
        recent = history.tail(3)
        template: Dict[str, Any] = {
            "event_month": target_month,
            "purchases_count": float(recent["purchases_count"].mean()),
            "avg_margin": float(recent["avg_margin"].mean()),
            "avg_sale_price": float(recent["avg_sale_price"].mean()),
            "avg_purchase_price": float(recent["avg_purchase_price"].mean()),
            "avg_days_inventory": float(recent["avg_days_inventory"].mean()),
            "inventory_rotation": float(recent["inventory_rotation"].mean()),
            "sales_count": float(history["sales_count"].iloc[-1]),
        }

        available_optional = [
            c for c in self.optional_category_cols if c in history.columns
        ]

        for col in self.category_cols + available_optional:
            template[col] = history[col].iloc[-1]

        future_history = pd.concat(
            [history, pd.DataFrame([template])], ignore_index=True
        )
        grouped_future = [
            self._add_lags(g.copy())
            for _, g in future_history.groupby(self.category_cols, sort=False)
        ]
        future_history = (
            pd.concat(grouped_future, ignore_index=True)
            if grouped_future
            else future_history
        )
        future_history = self._add_time_features(future_history)
        future_row = future_history[future_history["event_month"] == target_month].tail(
            1
        )
        future_row[self.numeric_cols] = future_row[self.numeric_cols].fillna(0)
        return future_row[
            self.category_cols
            + available_optional
            + self.numeric_cols
            + ["event_month"]
        ]

    def _add_lags(self, group: pd.DataFrame) -> pd.DataFrame:
        """Agrega columnas de lag y medias móviles al grupo."""
        group = group.sort_values("event_month")
        group["lag_1"] = group["sales_count"].shift(1)
        group["lag_3"] = group["sales_count"].shift(3)
        group["lag_6"] = group["sales_count"].shift(6)
        group["rolling_mean_3"] = (
            group["sales_count"].rolling(window=3, min_periods=1).mean().shift(1)
        )
        group["rolling_mean_6"] = (
            group["sales_count"].rolling(window=6, min_periods=1).mean().shift(1)
        )
        return group

    def _add_time_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Agrega features temporales (mes, año, representación cíclica)."""
        df["month"] = pd.DatetimeIndex(df["event_month"]).month
        df["year"] = pd.DatetimeIndex(df["event_month"]).year
        df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
        df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)
        return df
