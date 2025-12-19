from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from app.core.config import get_settings
from app.core.security import (
    require_internal_or_permissions,
    require_permissions,
    require_token,
)
from app.dependencies import get_prediction_service
from app.models.prediction_model import (
    MonthlyPrediction,
    PredictionRequest,
    PredictionResponse,
    PredictionWithHistoryResponse,
    RetrainRequest,
    RetrainResponse,
)

router = APIRouter(prefix="/v1/ml", tags=["prediction"])
settings = get_settings()
require_permissions_predict = (
    require_internal_or_permissions(settings.permissions_predict)
    if settings.permissions_predict
    else require_token
)
require_permissions_retrain = (
    require_internal_or_permissions(settings.permissions_retrain)
    if settings.permissions_retrain
    else require_token
)
require_permissions_models = (
    require_permissions(settings.permissions_models)
    if settings.permissions_models
    else require_token
)


@router.post(
    "/predict",
    response_model=PredictionResponse,
    dependencies=[Depends(require_permissions_predict)],
    summary="Predice demanda mensual por marca/modelo/linea",
)
async def predict(
    request: PredictionRequest, service=Depends(get_prediction_service)
) -> PredictionResponse:
    """Endpoint de inferencia principal para demanda mensual.

    Args:
        request: Segmento y horizonte solicitados.
        service: Servicio de predicción inyectado.
    """
    payload = await service.predict(
        filters=request.model_dump(exclude={"horizon_months", "confidence"}),
        horizon=request.horizon_months,
        confidence=request.confidence,
    )
    return PredictionResponse(**payload)


@router.post(
    "/predict-with-history",
    response_model=PredictionWithHistoryResponse,
    dependencies=[Depends(require_permissions_predict)],
    summary="Predice y entrega historial mensual para graficar",
)
async def predict_with_history(
    request: PredictionRequest, service=Depends(get_prediction_service)
) -> PredictionWithHistoryResponse:
    """Endpoint para frontend: pronóstico + serie histórica del segmento."""
    payload = await service.predict_with_history(
        filters=request.model_dump(exclude={"horizon_months", "confidence"}),
        horizon=request.horizon_months,
        confidence=request.confidence,
    )
    return PredictionWithHistoryResponse(**payload)


@router.post(
    "/retrain",
    response_model=RetrainResponse,
    dependencies=[Depends(require_permissions_retrain)],
    summary="Lanza un reentrenamiento con datos frescos",
)
async def retrain(
    body: RetrainRequest, service=Depends(get_prediction_service)
) -> RetrainResponse:
    """Reentrena el modelo de demanda con el historial actualizado."""
    try:
        metadata = await service.retrain(
            start_date=body.start_date, end_date=body.end_date
        )
    except ValueError as exc:
        # Entregamos error legible cuando falta historial suficiente.
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        ) from exc
    return RetrainResponse(
        version=metadata["version"],
        metrics=metadata.get("metrics", {}),
        trained_at=metadata.get("trained_at"),
        samples={
            "train": metadata.get("train_samples", 0),
            "test": metadata.get("test_samples", 0),
            "total": metadata.get("total_samples", 0),
        },
    )


@router.get(
    "/models/latest",
    dependencies=[Depends(require_permissions_models)],
    summary="Obtiene metadata del ultimo modelo",
)
async def latest_model(service=Depends(get_prediction_service)):
    """Devuelve la metadata del modelo activo en disco."""
    metadata = service.registry.latest_metadata()
    if not metadata:
        return {"detail": "No hay modelos disponibles"}
    return metadata
