from __future__ import annotations

import logging
from datetime import date
from typing import Any, Dict, List, Optional

import numpy as np
import pandas as pd
from fastapi import HTTPException, status

from app.core.config import Settings, get_settings
from app.database.repositories import FeatureStore, PredictionStore
from app.services.data_loader import DemandDatasetLoader
from app.services.normalization import (
    canonicalize_brand_model,
    canonicalize_label,
)
from app.services.model_registry import ModelRegistry
from app.services.training import TrainingService

logger = logging.getLogger(__name__)


class PredictionService:
    """Servicio de predicción y reentrenamiento de modelos de demanda.""" ""

    def __init__(
        self,
        loader: Optional[DemandDatasetLoader],
        trainer: TrainingService,
        registry: ModelRegistry,
        feature_store: FeatureStore | None = None,
        prediction_store: PredictionStore | None = None,
        settings: Settings | None = None,
    ) -> None:

        self.loader = loader
        self.trainer = trainer
        self.registry = registry
        self.feature_store = feature_store
        self.prediction_store = prediction_store
        self.settings = settings or get_settings()

    def _normalize_filters(self, filters: Dict[str, Any]) -> Dict[str, Any]:
        normalized_filters: Dict[str, Any] = {}
        normalized_filters["vehicle_type"] = canonicalize_label(
            filters.get("vehicle_type")
        )
        normalized_brand, normalized_model = canonicalize_brand_model(
            filters.get("brand"), filters.get("model")
        )
        normalized_filters["brand"] = normalized_brand
        normalized_filters["model"] = normalized_model
        normalized_filters["line"] = canonicalize_label(filters.get("line"))
        if not normalized_filters["line"]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Falta la línea del vehículo. Incluye brand/model/line completos para predecir.",
            )
        return normalized_filters

    def _filter_history(
        self, feature_df: pd.DataFrame, filters: Dict[str, Any]
    ) -> pd.DataFrame:
        mask_base = (
            (feature_df["vehicle_type"] == filters.get("vehicle_type"))
            & (feature_df["brand"] == filters.get("brand"))
            & (feature_df["model"] == filters.get("model"))
            & (feature_df["line"] == filters.get("line"))
        )
        return feature_df[mask_base].copy()

    async def _load_history(
        self, filters: Dict[str, Any], model_version: str
    ) -> pd.DataFrame:
        history = pd.DataFrame()
        if self.feature_store:
            history = self.feature_store.load_segment_history(model_version, filters)
            if not history.empty:
                return history

        if not self.loader:
            return history

        raw_df = await self.loader.load_transactions()
        feature_df = self.trainer.build_feature_table(raw_df)
        if feature_df.empty:
            return feature_df
        if self.feature_store and not feature_df.empty:
            self.feature_store.save_snapshot(model_version, feature_df)
        return self._filter_history(feature_df, filters)

    def _store_prediction(
        self,
        model_version: str,
        request_payload: Dict[str, Any],
        response_payload: Dict[str, Any],
        segment: Dict[str, Any],
        horizon: int,
        confidence: float,
        with_history: bool,
    ) -> None:
        if not self.prediction_store:
            return
        try:
            self.prediction_store.save_prediction(
                model_version=model_version,
                request_payload=request_payload,
                response_payload=response_payload,
                segment=segment,
                horizon=horizon,
                confidence=confidence,
                with_history=with_history,
            )
        except Exception as exc:
            logger.warning("Failed to save prediction: %s", exc)

    async def retrain(
        self, start_date: Optional[date] = None, end_date: Optional[date] = None
    ) -> Dict[str, Any]:
        if not self.loader:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="No hay loader de datos configurado para reentrenar.",
            )
        raw_df = await self.loader.load_transactions(
            start_date=start_date, end_date=end_date
        )
        if raw_df.empty:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No hay datos para entrenar en el rango solicitado.",
            )
        return self.trainer.train(raw_df)

    async def predict(
        self, filters: Dict[str, Any], horizon: int, confidence: float = 0.95
    ) -> Dict[str, Any]:

        if not self.loader and not self.feature_store:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="No hay loader de datos configurado para predecir.",
            )
        try:
            model, metadata = self.registry.load_latest()
        except FileNotFoundError as exc:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Aun no existe un modelo entrenado.",
            ) from exc

        normalized_filters = self._normalize_filters(filters)
        history = await self._load_history(normalized_filters, metadata["version"])

        if history.empty:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=(
                    "No se encontró historial para la combinación solicitada. "
                    "Verifica vehicle_type/brand/model/line o reentrena con datos que incluyan ese segmento."
                ),
            )

        history = history.sort_values("event_month")
        forecast = self._forecast(model, metadata, history, horizon, confidence)
        payload = {
            "predictions": forecast,
            "model_version": metadata["version"],
            "metrics": metadata.get("metrics"),
        }
        request_payload = {
            **filters,
            "horizon_months": horizon,
            "confidence": confidence,
        }
        self._store_prediction(
            model_version=metadata["version"],
            request_payload=request_payload,
            response_payload=payload,
            segment=normalized_filters,
            horizon=horizon,
            confidence=confidence,
            with_history=False,
        )
        return payload

    async def predict_with_history(
        self, filters: Dict[str, Any], horizon: int, confidence: float = 0.95
    ) -> Dict[str, Any]:
        if not self.loader and not self.feature_store:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="No hay loader de datos configurado para predecir.",
            )
        try:
            model, metadata = self.registry.load_latest()
        except FileNotFoundError as exc:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Aun no existe un modelo entrenado.",
            ) from exc

        normalized_filters = self._normalize_filters(filters)
        history = await self._load_history(normalized_filters, metadata["version"])

        if history.empty:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=(
                    "No se encontró historial para la combinación solicitada. "
                    "Verifica vehicle_type/brand/model/line o reentrena con datos que incluyan ese segmento."
                ),
            )

        history = history.sort_values("event_month")
        forecast = self._forecast(model, metadata, history, horizon, confidence)
        history_payload = [
            {
                "month": ts.date().isoformat(),
                "sales_count": float(sc),
            }
            for ts, sc in zip(history["event_month"], history["sales_count"])
        ]

        payload = {
            "predictions": forecast,
            "history": history_payload,
            "segment": {
                "vehicle_type": normalized_filters["vehicle_type"],
                "brand": normalized_filters["brand"],
                "model": normalized_filters["model"],
                "line": normalized_filters["line"],
            },
            "model_version": metadata["version"],
            "trained_at": metadata.get("trained_at"),
            "metrics": metadata.get("metrics"),
        }
        request_payload = {
            **filters,
            "horizon_months": horizon,
            "confidence": confidence,
        }
        self._store_prediction(
            model_version=metadata["version"],
            request_payload=request_payload,
            response_payload=payload,
            segment=normalized_filters,
            horizon=horizon,
            confidence=confidence,
            with_history=True,
        )
        return payload

    def _forecast(
        self,
        model: Any,
        metadata: Dict[str, Any],
        history: pd.DataFrame,
        horizon: int,
        confidence: float,
    ) -> List[Dict[str, Any]]:
        residual_std = metadata.get("metrics", {}).get("residual_std", 1.0)
        z_value = self._z_value(confidence)

        working_history = history.copy()
        target_month = working_history["event_month"].max()
        results: List[Dict[str, Any]] = []

        for _ in range(horizon):
            target_month = target_month + pd.offsets.MonthBegin(1)
            future_row = self.trainer.build_future_row(working_history, target_month)
            optional_cols = [
                col
                for col in getattr(self.trainer, "optional_category_cols", [])
                if col in future_row.columns
            ]
            features = future_row[
                self.trainer.category_cols + optional_cols + self.trainer.numeric_cols
            ]
            prediction = float(model.predict(features)[0])
            lower = max(0.0, prediction - z_value * residual_std)
            upper = max(lower, prediction + z_value * residual_std)

            results.append(
                {
                    "month": target_month.date().isoformat(),
                    "demand": prediction,
                    "lower_ci": lower,
                    "upper_ci": upper,
                }
            )

            appended = future_row.copy()
            appended["sales_count"] = prediction
            working_history = pd.concat([working_history, appended], ignore_index=True)

        return results

    def _z_value(self, confidence: float) -> float:
        conf = min(max(confidence, 0.5), 0.99)
        if conf >= 0.99:
            return 2.58
        if conf >= 0.95:
            return 1.96
        if conf >= 0.90:
            return 1.64
        if conf >= 0.80:
            return 1.28
        return 1.0
