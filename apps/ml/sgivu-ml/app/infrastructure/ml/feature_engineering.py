"""Ingeniería de features para modelos de demanda.

Responsabilidad única: transformar transacciones brutas en features
numéricas y categóricas aptas para modelos de ML.
"""

from __future__ import annotations

import logging
from typing import Any, Dict

import numpy as np
import pandas as pd

from app.infrastructure.config import Settings
from app.infrastructure.ml.data_validation import clip_outliers, validate_raw_data
from app.infrastructure.ml.normalization import (
    canonicalize_brand_model,
    canonicalize_label_or_unknown,
)

logger = logging.getLogger(__name__)


class FeatureEngineering:
    """Construye tablas de features a partir de transacciones brutas."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.category_cols: list[str] = ["vehicle_type", "brand", "model", "line"]
        self.optional_category_cols: list[str] = []
        # `year` se omite intencionalmente: como entero monotónico no generaliza
        # fuera del rango visto en train (StandardScaler lo normaliza con la
        # media de los años de entrenamiento, y los árboles no extrapolan).
        # La estacionalidad ya queda capturada por month/month_sin/month_cos
        # y por los lags y rolling means.
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
            "month_sin",
            "month_cos",
            "quarter",
            "quarter_sin",
            "quarter_cos",
        ]
        self.direct_numeric_cols: list[str] = self.numeric_cols + ["horizon_step"]

    def build_feature_table(self, df: pd.DataFrame) -> pd.DataFrame:
        """Transforma transacciones brutas en una tabla de features mensual.

        Realiza validación, normalización, agregación mensual, adición de lags
        y features temporales. El resultado está listo para alimentar un
        pipeline de ML que espere columnas categóricas y numéricas.

        Parameters
        ----------
        df : pd.DataFrame
            DataFrame de transacciones crudas tal como lo devuelve
            `HttpTransactionLoader`; debe contener al menos `created_at` o
            `updated_at`, `contractType`, `salePrice`, `purchasePrice` y `line`.

        Returns
        -------
        pd.DataFrame
            DataFrame agregado por `event_month` con columnas categóricas
            y las columnas numéricas listadas en `self.numeric_cols`.

        Raises
        ------
        ValueError
            Si falta la columna `line` o si existen registros con `line` vacío.
        """
        if df.empty:
            return df

        work_df = self._validate_and_canonicalize(df)
        work_df = self._add_temporal_columns(work_df)
        monthly = self._aggregate_monthly(work_df)
        monthly = self._add_lags_and_cold_start(monthly)
        monthly[self.numeric_cols] = monthly[self.numeric_cols].fillna(0)
        return monthly

    def _validate_and_canonicalize(self, df: pd.DataFrame) -> pd.DataFrame:
        """Valida datos y normaliza columnas categóricas."""
        work_df, warnings = validate_raw_data(df)
        for warning in warnings:
            logger.warning(warning)

        work_df = work_df.copy()
        if "line" not in work_df.columns:
            raise ValueError(
                "La columna 'line' es obligatoria para entrenar el modelo."
            )

        for col in self.category_cols + self.optional_category_cols:
            if col in work_df:
                work_df[col] = work_df[col].apply(canonicalize_label_or_unknown)

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

        return work_df

    def _add_temporal_columns(self, work_df: pd.DataFrame) -> pd.DataFrame:
        """Agrega columnas de fecha, tipo de contrato e inventario."""
        # Acepta cadenas ISO8601 con o sin fracciones de segundo (p. ej. .668838)
        # para evitar fallos al mezclar precisiones en una misma columna.
        work_df["event_date"] = pd.to_datetime(
            work_df["updated_at"].fillna(work_df["created_at"]),
            utc=True,
            format="ISO8601",
        ).dt.tz_localize(None)
        work_df["event_month"] = (
            pd.DatetimeIndex(work_df["event_date"]).to_period("M").to_timestamp()
        )

        work_df["is_sale"] = work_df["contract_type"] == "SALE"
        work_df["is_purchase"] = work_df["contract_type"] == "PURCHASE"
        work_df["margin"] = work_df["sale_price"] - work_df["purchase_price"]

        purchases = work_df[work_df["is_purchase"]]
        if not purchases.empty:
            purchase_dates = (
                purchases.sort_values("event_date")
                .drop_duplicates("vehicle_id", keep="first")
                .set_index("vehicle_id")["event_date"]
            )
            work_df["purchase_date"] = work_df["vehicle_id"].map(purchase_dates)
        else:
            work_df["purchase_date"] = pd.NaT
        work_df["days_in_inventory"] = np.where(
            work_df["is_sale"] & work_df["purchase_date"].notna(),
            (work_df["event_date"].to_numpy() - work_df["purchase_date"].to_numpy())
            / np.timedelta64(1, "D"),
            np.nan,
        )
        return work_df

    def _aggregate_monthly(self, work_df: pd.DataFrame) -> pd.DataFrame:
        """Agrega transacciones por mes/segmento y aplica clipping de outliers."""
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

        monthly["inventory_rotation"] = np.where(
            monthly["purchases_count"] > 0,
            monthly["sales_count"] / monthly["purchases_count"],
            0.0,
        )

        iqr_mult = self.settings.outlier_iqr_multiplier
        for col in ("sales_count", "purchases_count"):
            monthly = clip_outliers(monthly, col, iqr_mult)

        return self._add_time_features(monthly)

    def _add_lags_and_cold_start(self, monthly: pd.DataFrame) -> pd.DataFrame:
        """Agrega lags, rolling means y aplica fallback de cold-start."""
        global_monthly_means = monthly.groupby("event_month")["sales_count"].mean()

        grouped_frames = [
            self._add_lags(g.copy())
            for _, g in monthly.groupby(self.category_cols, sort=False)
        ]
        monthly = (
            pd.concat(grouped_frames, ignore_index=True)
            if grouped_frames
            else pd.DataFrame()
        )

        lag_cols = ["lag_1", "lag_3", "lag_6", "rolling_mean_3", "rolling_mean_6"]
        for col in lag_cols:
            if col not in monthly:
                monthly[col] = np.nan

        monthly = monthly.sort_values("event_month")

        cold_start_threshold = self.settings.cold_start_min_months
        for _, group in monthly.groupby(self.category_cols, sort=False):
            if len(group) < cold_start_threshold:
                global_mean = float(global_monthly_means.mean())
                for col in lag_cols:
                    monthly.loc[group.index, col] = monthly.loc[
                        group.index, col
                    ].fillna(global_mean)

        return monthly

    def build_future_row(
        self, history: pd.DataFrame, target_month: pd.Timestamp
    ) -> pd.DataFrame:
        """Construye una fila de features para un mes futuro basándose en el historial.

        Parameters
        ----------
        history : pd.DataFrame
            Historial de features para el segmento objetivo (resultado de
            `build_feature_table` o `load_segment_history`).
        target_month : pd.Timestamp
            Marca temporal del mes futuro para el que se calcularán features.

        Returns
        -------
        pd.DataFrame
            DataFrame de una sola fila (puede incluir columnas opcionales)
            listo para ser pasado al modelo.

        Raises
        ------
        ValueError
            Si `history` está vacío.
        """
        if history.empty:
            raise ValueError("No hay historial para calcular predicciones.")

        history = history.sort_values("event_month")
        recent = history.tail(self.settings.lag_window_short)
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
        s = self.settings.lag_window_short
        l = self.settings.lag_window_long
        group = group.sort_values("event_month")
        group["lag_1"] = group["sales_count"].shift(1)
        group["lag_3"] = group["sales_count"].shift(s)
        group["lag_6"] = group["sales_count"].shift(l)
        group["rolling_mean_3"] = (
            group["sales_count"].rolling(window=s, min_periods=1).mean().shift(1)
        )
        group["rolling_mean_6"] = (
            group["sales_count"].rolling(window=l, min_periods=1).mean().shift(1)
        )
        return group

    def _add_time_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Agrega features temporales (mes, trimestre, representación cíclica)."""
        df["month"] = pd.DatetimeIndex(df["event_month"]).month
        df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
        df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)
        df["quarter"] = (df["month"] - 1) // 3 + 1
        df["quarter_sin"] = np.sin(2 * np.pi * df["quarter"] / 4)
        df["quarter_cos"] = np.cos(2 * np.pi * df["quarter"] / 4)
        return df

    def build_direct_feature_table(
        self, monthly_df: pd.DataFrame, max_horizon: int
    ) -> pd.DataFrame:
        """Construye dataset de entrenamiento multi-step directo.

        Por cada fila en tiempo t genera hasta ``max_horizon`` filas: una por
        cada horizonte h (1..max_horizon) en el que existe un valor real de
        ``sales_count`` en t+h dentro del mismo segmento.  Las features de
        lag permanecen ancladas en t (datos reales), eliminando la
        acumulación de error del enfoque recursivo.  La columna
        ``horizon_step`` indica al modelo qué horizonte debe predecir.

        Parameters
        ----------
        monthly_df : pd.DataFrame
            Tabla mensual producida por ``build_feature_table``.
        max_horizon : int
            Número máximo de pasos adelante a generar.

        Returns
        -------
        pd.DataFrame
            Dataset expandido con columna ``horizon_step`` y ``sales_count``
            igual al valor real en t+h.
        """
        rows: list[dict] = []
        for _, group in monthly_df.groupby(self.category_cols, sort=False):
            group = group.sort_values("event_month").reset_index(drop=True)
            sales_values = group["sales_count"].tolist()
            for i, row in group.iterrows():
                for h in range(1, max_horizon + 1):
                    target_idx = i + h
                    if target_idx >= len(group):
                        break
                    new_row = row.to_dict()
                    new_row["horizon_step"] = float(h)
                    new_row["sales_count"] = sales_values[target_idx]
                    rows.append(new_row)
        if not rows:
            return pd.DataFrame()
        return pd.DataFrame(rows).reset_index(drop=True)

    def build_future_row_at_origin(
        self,
        history: pd.DataFrame,
        target_month: pd.Timestamp,
        horizon_step: int,
    ) -> pd.DataFrame:
        """Construye una fila de features para predicción directa (sin recursión).

        A diferencia de ``build_future_row``, los lags se leen siempre del
        historial real congelado en el origen de la predicción, sin usar
        valores predichos previamente.  Esto elimina la acumulación de error
        en pronósticos multi-step.

        Parameters
        ----------
        history : pd.DataFrame
            Historial real del segmento (congelado, nunca modificado por
            predicciones anteriores).
        target_month : pd.Timestamp
            Mes objetivo para el que se calculan las features temporales.
        horizon_step : int
            Paso de horizonte (h), pasado como feature al modelo.

        Returns
        -------
        pd.DataFrame
            DataFrame de una sola fila con columnas ``direct_numeric_cols``
            y las columnas categóricas del segmento.
        """
        if history.empty:
            raise ValueError("No hay historial para calcular predicciones.")

        s = self.settings.lag_window_short
        l = self.settings.lag_window_long
        history = history.sort_values("event_month")
        recent = history.tail(s)

        lag_series = history["sales_count"]
        lag_1 = float(lag_series.iloc[-1]) if len(lag_series) >= 1 else 0.0
        lag_3 = (
            float(lag_series.iloc[-s])
            if len(lag_series) >= s
            else float(lag_series.iloc[0])
        )
        lag_6 = (
            float(lag_series.iloc[-l])
            if len(lag_series) >= l
            else float(lag_series.iloc[0])
        )
        rolling_3 = float(lag_series.tail(s).mean())
        rolling_6 = float(lag_series.tail(l).mean())

        row: dict = {
            "purchases_count": float(recent["purchases_count"].mean()),
            "avg_margin": float(recent["avg_margin"].mean()),
            "avg_sale_price": float(recent["avg_sale_price"].mean()),
            "avg_purchase_price": float(recent["avg_purchase_price"].mean()),
            "avg_days_inventory": float(recent["avg_days_inventory"].mean()),
            "inventory_rotation": float(recent["inventory_rotation"].mean()),
            "lag_1": lag_1,
            "lag_3": lag_3,
            "lag_6": lag_6,
            "rolling_mean_3": rolling_3,
            "rolling_mean_6": rolling_6,
            "horizon_step": float(horizon_step),
        }

        month_val = target_month.month
        quarter_val = (month_val - 1) // 3 + 1
        row["month"] = float(month_val)
        row["month_sin"] = float(np.sin(2 * np.pi * month_val / 12))
        row["month_cos"] = float(np.cos(2 * np.pi * month_val / 12))
        row["quarter"] = float(quarter_val)
        row["quarter_sin"] = float(np.sin(2 * np.pi * quarter_val / 4))
        row["quarter_cos"] = float(np.cos(2 * np.pi * quarter_val / 4))

        for col in self.category_cols + self.optional_category_cols:
            if col in history.columns:
                row[col] = history[col].iloc[-1]

        df_row = pd.DataFrame([row])
        df_row["event_month"] = target_month

        available_optional = [
            c for c in self.optional_category_cols if c in history.columns
        ]
        cols = (
            self.category_cols
            + available_optional
            + self.direct_numeric_cols
            + ["event_month"]
        )
        for col in cols:
            if col not in df_row.columns:
                df_row[col] = 0.0
        return df_row[cols]
