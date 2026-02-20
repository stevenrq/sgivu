from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any, Dict, List, Tuple, cast

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import (
    mean_absolute_error,
    mean_absolute_percentage_error,
    mean_squared_error,
    r2_score,
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.core.config import Settings, get_settings
from app.database.repositories import FeatureStore
from app.services.normalization import (
    canonicalize_brand_model,
    canonicalize_label,
)
from app.services.model_registry import ModelRegistry

try:
    from xgboost import XGBRegressor
except Exception:
    XGBRegressor = None

logger = logging.getLogger(__name__)


class TrainingService:
    """Servicio de entrenamiento y versionado de modelos de demanda."""

    category_cols = ["vehicle_type", "brand", "model", "line"]
    optional_category_cols: list[str] = []
    numeric_cols = [
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

    def __init__(
        self,
        registry: ModelRegistry,
        feature_store: FeatureStore | None = None,
        settings: Settings | None = None,
    ) -> None:
        self.registry = registry
        self.feature_store = feature_store
        self.settings = settings or get_settings()

    def build_feature_table(self, df: pd.DataFrame) -> pd.DataFrame:
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
            # Consolida alias de marca/modelo para evitar segmentos duplicados.
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
        # Margen simple usado para promedios y rotación económica.
        work_df["margin"] = work_df["sale_price"] - work_df["purchase_price"]

        purchase_dates = (
            work_df[work_df["is_purchase"]]
            .sort_values("event_date")
            .drop_duplicates("vehicle_id", keep="first")
            .set_index("vehicle_id")["event_date"]
        )
        # Mapear la fecha de compra inicial al resto de eventos del mismo vehículo.
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

    def _add_lags(self, group: pd.DataFrame) -> pd.DataFrame:
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
        df["month"] = pd.DatetimeIndex(df["event_month"]).month
        df["year"] = pd.DatetimeIndex(df["event_month"]).year
        df["month_sin"] = np.sin(2 * np.pi * df["month"] / 12)
        df["month_cos"] = np.cos(2 * np.pi * df["month"] / 12)
        return df

    def _split_by_time(self, df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Divide en train/test respetando el orden temporal del historial."""
        unique_months = sorted(df["event_month"].unique())
        if len(unique_months) < self.settings.min_history_months:
            raise ValueError(
                f"Se requieren al menos {self.settings.min_history_months} meses para entrenar."
            )
        if len(unique_months) == 1:
            return df.copy(), df.copy()
        cutoff_index = int(len(unique_months) * 0.8)
        cutoff_date = unique_months[max(1, cutoff_index - 1)]
        train = df[df["event_month"] <= cutoff_date]
        test = df[df["event_month"] > cutoff_date]
        if test.empty:
            test = train.tail(max(1, len(train) // 5))
            train = train.drop(test.index)
        return train, test

    def _build_preprocessor(self, optional_cols: List[str]) -> ColumnTransformer:
        categorical = OneHotEncoder(handle_unknown="ignore")
        numeric = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="median")),
                ("scaler", StandardScaler()),
            ]
        )
        # Solo añade categorías opcionales presentes para evitar fallos cuando faltan columnas.
        return ColumnTransformer(
            transformers=[
                ("categorical", categorical, self.category_cols + optional_cols),
                ("numeric", numeric, self.numeric_cols),
            ],
            remainder="drop",
        )

    def _candidate_models(self) -> List[Tuple[str, Any]]:
        models: List[Tuple[str, Any]] = [
            ("linear_regression", LinearRegression()),
            (
                "random_forest",
                RandomForestRegressor(n_estimators=300, max_depth=15, random_state=7),
            ),
        ]
        if XGBRegressor:
            models.append(
                (
                    "xgboost",
                    XGBRegressor(
                        n_estimators=500,
                        max_depth=6,
                        learning_rate=0.05,
                        subsample=0.9,
                        colsample_bytree=0.9,
                        objective="reg:squarederror",
                        random_state=7,
                    ),
                )
            )
        return models

    def train(self, df: pd.DataFrame) -> Dict[str, Any]:
        dataset = self.build_feature_table(df)
        if dataset.empty:
            raise ValueError("No hay datos historicos para entrenar.")

        train_df, test_df = self._split_by_time(dataset)

        optional_cols = [c for c in self.optional_category_cols if c in dataset.columns]

        X_train = train_df[self.category_cols + optional_cols + self.numeric_cols]
        y_train = train_df["sales_count"]
        X_test = test_df[self.category_cols + optional_cols + self.numeric_cols]
        y_test = test_df["sales_count"]

        preprocessor = self._build_preprocessor(optional_cols)
        candidates = self._candidate_models()

        evaluated: List[Dict[str, Any]] = []
        best_model: Pipeline | None = None
        best_rmse = np.inf
        best_metrics: Dict[str, float] = {}
        best_predictions: np.ndarray | None = None
        sample_count = len(y_test)

        def _safe_metric(value: float) -> float:
            return float(value) if np.isfinite(value) else 0.0

        for name, estimator in candidates:
            # memory=None se declara explícitamente para cumplir guideline de cacheo en sklearn Pipeline.
            pipeline = Pipeline(
                steps=[("preprocess", preprocessor), ("model", estimator)],
                memory=None,
            )
            pipeline.fit(X_train, y_train)
            preds = np.asarray(pipeline.predict(X_test))
            rmse = _safe_metric(np.sqrt(mean_squared_error(y_test, preds)))
            mae = _safe_metric(mean_absolute_error(y_test, preds))
            safe_y_test = y_test.replace(0, 1e-3)
            mape = _safe_metric(mean_absolute_percentage_error(safe_y_test, preds))
            if sample_count >= 2:
                r2 = _safe_metric(r2_score(y_test, preds))
            else:
                # Evitamos warnings de sklearn cuando solo hay un punto en test.
                logger.debug("Skipping r2: only %s sample(s) in test", sample_count)
                r2 = 0.0

            evaluated.append(
                {
                    "model": name,
                    "rmse": rmse,
                    "mae": mae,
                    "mape": mape,
                    "r2": r2,
                    "samples": sample_count,
                }
            )

            if rmse < best_rmse:
                best_rmse = rmse
                best_model = pipeline
                best_metrics = {"rmse": rmse, "mae": mae, "mape": mape, "r2": r2}
                best_predictions = preds

        assert best_model is not None

        final_model = best_model.fit(
            dataset[self.category_cols + optional_cols + self.numeric_cols],
            dataset["sales_count"],
        )
        residuals = y_test - best_predictions if best_predictions is not None else []
        residual_std = float(np.std(residuals)) if len(residuals) else 1.0

        metadata = {
            "trained_at": datetime.now(timezone.utc).isoformat(),
            "target": self.settings.target_column,
            "features": self.category_cols + self.numeric_cols,
            "metrics": {**best_metrics, "residual_std": residual_std},
            "candidates": evaluated,
            "train_samples": len(train_df),
            "test_samples": len(test_df),
            "total_samples": len(dataset),
        }

        saved_metadata = self.registry.save(final_model, metadata)
        if self.feature_store:
            self.feature_store.save_snapshot(saved_metadata["version"], dataset)
        logger.info("Model trained and versioned: %s", saved_metadata["version"])
        return saved_metadata

    def build_future_row(
        self, history: pd.DataFrame, target_month: pd.Timestamp
    ) -> pd.DataFrame:
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
