"""Servicio de predicción de demanda.

Orquesta los casos de uso de predicción, predicción-con-historial,
reentrenamiento y consulta de metadata del modelo.  Depende únicamente
de puertos (Protocol) para los repositorios y el registro de modelos,
cumpliendo con el Principio de Inversión de Dependencias (DIP).
"""

from __future__ import annotations

import logging
from datetime import date
from typing import Any, Dict, List, Optional

import pandas as pd
from scipy.stats import norm

from app.application.services.training_service import TrainingService
from app.domain.entities import (
    ForecastPoint,
    HistoricalPoint,
    ModelMetadata,
    PredictionResult,
    PredictionWithHistoryResult,
    VehicleSegment,
)
from app.domain.exceptions import (
    DataLoadError,
    InsufficientHistoryError,
    MissingVehicleLineError,
    ModelNotTrainedError,
    SegmentNotFoundError,
)
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
from app.domain.ports.prediction_repository import PredictionRepositoryPort
from app.domain.ports.transaction_loader import TransactionLoaderPort
from app.infrastructure.config import Settings, get_settings
from app.infrastructure.ml.feature_engineering import FeatureEngineering
from app.infrastructure.ml.normalization import (
    canonicalize_brand_model,
    canonicalize_label,
)

logger = logging.getLogger(__name__)


class PredictionService:
    """Casos de uso: predecir demanda, reentrenar y consultar modelos.

    Responsable de orquestar flujos de predicción y persistencia de
    resultados. Opera contra puertos (ports) y no realiza I/O directo
    fuera de las abstracciones inyectadas.

    Attributes
    ----------
    _registry : ModelRegistryPort
        Puerto para cargar/guardar artefactos del modelo.
    _feature_engineering : FeatureEngineering
        Construye features a partir de transacciones brutas.
    _training_service : TrainingService
        Servicio orquestador para reentrenamiento.
    _transaction_loader : TransactionLoaderPort | None
        Opcional: cargador de transacciones si se necesita ingesta directa.
    _feature_repository : FeatureRepositoryPort | None
        Opcional: repositorio de snapshots de features.
    _prediction_repository : PredictionRepositoryPort | None
        Opcional: repositorio para guardar registros de predicción.
    _settings : Settings
        Configuración de la aplicación.
    """

    def __init__(
        self,
        registry: ModelRegistryPort,
        feature_engineering: FeatureEngineering,
        training_service: TrainingService,
        transaction_loader: TransactionLoaderPort | None = None,
        feature_repository: FeatureRepositoryPort | None = None,
        prediction_repository: PredictionRepositoryPort | None = None,
        settings: Settings | None = None,
    ) -> None:
        self._registry = registry
        self._feature_engineering = feature_engineering
        self._training_service = training_service
        self._transaction_loader = transaction_loader
        self._feature_repository = feature_repository
        self._prediction_repository = prediction_repository
        self._settings = settings or get_settings()

    # ------------------------------------------------------------------
    # Casos de uso públicos
    # ------------------------------------------------------------------

    async def predict(
        self,
        filters: Dict[str, Any],
        horizon: int,
        confidence: float = 0.95,
    ) -> PredictionResult:
        """Predice demanda mensual para un segmento de vehículo.

        Parameters
        ----------
        filters : dict
            Diccionario con los filtros del segmento (puede incluir
            `vehicle_type`, `brand`, `model`, `line`).
        horizon : int
            Horizonte de predicción en meses.
        confidence : float, optional
            Nivel de confianza para los intervalos (por defecto 0.95).

        Returns
        -------
        PredictionResult
            Resultado con la lista de predicciones, versión del modelo y métricas.

        Raises
        ------
        DataLoadError
            Si no hay fuente de datos configurada para la predicción.
        ModelNotTrainedError
            Si no existe un modelo entrenado disponible.
        SegmentNotFoundError
            Si no se encuentra historial para el segmento especificado.
        """
        self._require_data_source()
        model, metadata = await self._load_model()

        segment = self._normalize_filters(filters)
        history = await self._load_history(segment, metadata.version)
        self._require_history(history)

        history = history.sort_values("event_month")
        forecast = self._forecast(model, metadata, history, horizon, confidence)

        result = PredictionResult(
            predictions=[ForecastPoint(**f) for f in forecast],
            model_version=metadata.version,
            metrics=metadata.metrics,
        )

        await self._store_prediction(
            metadata.version,
            {**filters, "horizon_months": horizon, "confidence": confidence},
            result.model_dump(),
            segment,
            horizon,
            confidence,
            with_history=False,
        )
        return result

    async def predict_with_history(
        self,
        filters: Dict[str, Any],
        horizon: int,
        confidence: float = 0.95,
    ) -> PredictionWithHistoryResult:
        """Predice demanda y devuelve también el historial usado para graficar.

        Parameters
        ----------
        filters : dict
            Filtros del segmento (vehicle_type, brand, model, line).
        horizon : int
            Horizonte de predicción en meses.
        confidence : float, optional
            Nivel de confianza para los intervalos (por defecto 0.95).

        Returns
        -------
        PredictionWithHistoryResult
            Resultado con predicciones, historial (lista de `HistoricalPoint`),
            metadata del modelo y métricas.

        Raises
        ------
        DataLoadError, ModelNotTrainedError, SegmentNotFoundError
            Comportamiento análogo a `predict`.
        """
        self._require_data_source()
        model, metadata = await self._load_model()

        segment = self._normalize_filters(filters)
        history = await self._load_history(segment, metadata.version)
        self._require_history(history)

        history = history.sort_values("event_month")
        forecast = self._forecast(model, metadata, history, horizon, confidence)
        history_points = [
            HistoricalPoint(month=ts.date().isoformat(), sales_count=float(sc))
            for ts, sc in zip(history["event_month"], history["sales_count"])
        ]

        result = PredictionWithHistoryResult(
            predictions=[ForecastPoint(**f) for f in forecast],
            history=history_points,
            segment=segment,
            model_version=metadata.version,
            trained_at=metadata.trained_at,
            metrics=metadata.metrics,
        )

        await self._store_prediction(
            metadata.version,
            {**filters, "horizon_months": horizon, "confidence": confidence},
            result.model_dump(),
            segment,
            horizon,
            confidence,
            with_history=True,
        )
        return result

    async def retrain(
        self,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> ModelMetadata:
        """Lanza un reentrenamiento con datos frescos.

        Parameters
        ----------
        start_date : date | None
            Fecha de inicio (inclusive) para filtrar transacciones.
        end_date : date | None
            Fecha de fin (inclusive) para filtrar transacciones.

        Returns
        -------
        ModelMetadata
            Metadata del modelo entrenado y versionado.

        Raises
        ------
        DataLoadError
            Si no hay loader de datos configurado para reentrenamiento.
        InsufficientHistoryError
            Si no hay datos suficientes para entrenar en el rango solicitado.
        TrainingError
            Si el proceso de entrenamiento falla internamente.
        """
        if not self._transaction_loader:
            raise DataLoadError("No hay loader de datos configurado para reentrenar.")
        raw_df = await self._transaction_loader.load_transactions(
            start_date=start_date, end_date=end_date
        )
        if raw_df.empty:
            raise InsufficientHistoryError(
                "No hay datos para entrenar en el rango solicitado."
            )
        return await self._training_service.train(raw_df)

    async def get_latest_model(self) -> ModelMetadata | None:
        """Obtiene la metadata del último modelo entrenado.

        Returns
        -------
        ModelMetadata | None
            Metadata del último modelo o `None` si no existe ninguno.
        """
        return await self._registry.latest_metadata()

    # ------------------------------------------------------------------
    # Métodos privados
    # ------------------------------------------------------------------

    def _require_data_source(self) -> None:
        if not self._transaction_loader and not self._feature_repository:
            raise DataLoadError("No hay loader de datos configurado para predecir.")

    async def _load_model(self) -> tuple[Any, ModelMetadata]:
        try:
            return await self._registry.load_latest()
        except FileNotFoundError as exc:
            raise ModelNotTrainedError("Aún no existe un modelo entrenado.") from exc

    def _normalize_filters(self, filters: Dict[str, Any]) -> VehicleSegment:
        vehicle_type = canonicalize_label(filters.get("vehicle_type"))
        brand, model = canonicalize_brand_model(
            filters.get("brand"), filters.get("model")
        )
        line = canonicalize_label(filters.get("line"))
        if not line:
            raise MissingVehicleLineError(
                "Falta la línea del vehículo. "
                "Incluye brand/model/line completos para predecir."
            )
        return VehicleSegment(
            vehicle_type=vehicle_type, brand=brand, model=model, line=line
        )

    @staticmethod
    def _require_history(history: pd.DataFrame) -> None:
        if history.empty:
            raise SegmentNotFoundError(
                "No se encontró historial para la combinación solicitada. "
                "Verifica vehicle_type/brand/model/line o reentrena con datos "
                "que incluyan ese segmento."
            )

    async def _load_history(
        self, segment: VehicleSegment, model_version: str
    ) -> pd.DataFrame:
        """Intenta cargar historial desde el repositorio; si no, desde el loader."""
        if self._feature_repository:
            history = await self._feature_repository.load_segment_history(
                model_version, segment.model_dump()
            )
            if not history.empty:
                return history

        if not self._transaction_loader:
            return pd.DataFrame()

        raw_df = await self._transaction_loader.load_transactions()
        feature_df = self._feature_engineering.build_feature_table(raw_df)
        if feature_df.empty:
            return feature_df
        if self._feature_repository:
            await self._feature_repository.save_snapshot(model_version, feature_df)
        return self._filter_history(feature_df, segment)

    @staticmethod
    def _filter_history(
        feature_df: pd.DataFrame, segment: VehicleSegment
    ) -> pd.DataFrame:
        mask = (
            (feature_df["vehicle_type"] == segment.vehicle_type)
            & (feature_df["brand"] == segment.brand)
            & (feature_df["model"] == segment.model)
            & (feature_df["line"] == segment.line)
        )
        return feature_df[mask].copy()

    async def _store_prediction(
        self,
        model_version: str,
        request_payload: Dict[str, Any],
        response_payload: Dict[str, Any],
        segment: VehicleSegment,
        horizon: int,
        confidence: float,
        with_history: bool,
    ) -> None:
        if not self._prediction_repository:
            return
        try:
            await self._prediction_repository.save_prediction(
                model_version=model_version,
                request_payload=request_payload,
                response_payload=response_payload,
                segment=segment.model_dump(),
                horizon=horizon,
                confidence=confidence,
                with_history=with_history,
            )
        except Exception as exc:
            logger.warning("Failed to save prediction: %s", exc)

    def _forecast(
        self,
        model: Any,
        metadata: ModelMetadata,
        history: pd.DataFrame,
        horizon: int,
        confidence: float,
    ) -> List[Dict[str, Any]]:
        """Genera pronóstico directo multi-step usando el modelo entrenado.

        Los lags se leen del historial real congelado en el origen de la
        predicción: no se usan predicciones anteriores como features, por
        lo que no hay acumulación de error entre pasos.
        """
        metrics = metadata.metrics or {}
        z_value = self._z_value(confidence)
        baseline = self._compute_segment_baseline(history)

        rate = self._settings.forecast_dampening_rate
        floor = self._settings.forecast_dampening_floor

        fe = self._feature_engineering
        frozen_history = history.copy()
        origin_month = frozen_history["event_month"].max()
        results: List[Dict[str, Any]] = []

        for step in range(1, horizon + 1):
            target_month = origin_month + pd.offsets.MonthBegin(step)
            future_row = fe.build_future_row_at_origin(
                frozen_history, target_month, step
            )

            model_features = metadata.features or (
                fe.category_cols + fe.direct_numeric_cols
            )
            optional_cols = [
                col for col in fe.optional_category_cols if col in future_row.columns
            ]
            all_cols = fe.category_cols + optional_cols + fe.direct_numeric_cols
            compatible_cols = [
                c for c in all_cols if c in model_features and c in future_row.columns
            ]
            features = future_row[compatible_cols]
            raw_prediction = float(model.predict(features)[0])

            alpha = max(floor, 1.0 - rate * step)
            prediction = alpha * raw_prediction + (1 - alpha) * baseline

            step_std = self._resolve_horizon_residual(metrics, step)
            margin = z_value * step_std
            lower = max(0.0, prediction - margin)
            upper = max(lower, prediction + margin)

            results.append(
                {
                    "month": target_month.date().isoformat(),
                    "demand": prediction,
                    "lower_ci": lower,
                    "upper_ci": upper,
                }
            )

        return results

    def _compute_segment_baseline(self, history: pd.DataFrame) -> float:
        """Media de ventas de los últimos lag_window_long meses como baseline para dampening."""
        if history.empty:
            return 0.0
        recent = history.tail(self._settings.lag_window_long)["sales_count"]
        return float(recent.mean())

    @staticmethod
    def _resolve_segment_residual(
        history: pd.DataFrame, metrics: Dict[str, Any]
    ) -> float:
        """Obtiene residual_std específico del segmento o el global como fallback."""
        segment_residuals = metrics.get("segment_residuals", {})
        if segment_residuals and not history.empty:
            last = history.iloc[-1]
            seg_key = "|".join(
                str(last.get(c, "")) for c in ["vehicle_type", "brand", "model", "line"]
            )
            if seg_key in segment_residuals:
                return float(segment_residuals[seg_key])
        return float(metrics.get("residual_std", 1.0))

    @staticmethod
    def _resolve_horizon_residual(metrics: Dict[str, Any], step: int) -> float:
        """Obtiene residual_std calibrado para el horizonte ``step``.

        Prioriza ``horizon_residuals[step]`` (calculado por horizonte durante
        el entrenamiento directo multi-step).  Si no existe, usa el
        ``residual_std`` global como fallback.
        """
        horizon_residuals = metrics.get("horizon_residuals", {})
        if horizon_residuals:
            h_std = horizon_residuals.get(step) or horizon_residuals.get(str(step))
            if h_std is not None:
                return float(h_std)
        return float(metrics.get("residual_std", 1.0))

    @staticmethod
    def _z_value(confidence: float) -> float:
        """Valor z para un nivel de confianza dado usando scipy."""
        conf = min(max(confidence, 0.5), 0.99)
        return float(norm.ppf((1 + conf) / 2))
