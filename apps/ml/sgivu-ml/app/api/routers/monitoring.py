"""Router de monitoreo de drift y feature importance.

Endpoints para registrar valores reales, consultar drift reports
e inspeccionar las importancias de features del modelo actual.
"""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException, Query

from app.api.dependencies import get_monitoring_service, get_prediction_service
from app.api.schemas.monitoring import (
    ActualDemandRequest,
    ActualDemandResponse,
    DriftReportResponse,
)
from app.application.services.monitoring_service import MonitoringService
from app.application.services.prediction_service import PredictionService
from app.infrastructure.config import get_settings
from app.infrastructure.security.auth_dependencies import (
    require_internal_or_permissions,
    require_permissions,
    require_token,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/ml", tags=["monitoring"])
settings = get_settings()

require_permissions_models = (
    require_permissions(settings.permissions_models)
    if settings.permissions_models
    else require_token
)
require_permissions_retrain = (
    require_internal_or_permissions(settings.permissions_retrain)
    if settings.permissions_retrain
    else require_token
)


@router.post(
    "/actuals",
    response_model=ActualDemandResponse,
    dependencies=[Depends(require_permissions_retrain)],
    summary="Registra demanda real para monitoreo de drift",
)
async def record_actual(
    request: ActualDemandRequest,
    service: MonitoringService | None = Depends(get_monitoring_service),
) -> ActualDemandResponse:
    """Registra una observación real de demanda para monitoreo.

    Parameters
    ----------
    request : ActualDemandRequest
        Payload con la demanda real y metadatos del segmento.
    service : MonitoringService | None
        Servicio de monitoreo inyectado. Si es `None`, se devuelve 503.

    Returns
    -------
    ActualDemandResponse
        Respuesta con la versión del modelo y el mes registrado.
    """
    if service is None:
        raise HTTPException(
            status_code=503,
            detail="Servicio de monitoreo no disponible: base de datos no configurada.",
        )
    segment = {
        "vehicle_type": request.vehicle_type,
        "brand": request.brand,
        "model": request.model,
        "line": request.line,
    }
    await service.record_actual(
        model_version=request.model_version,
        segment=segment,
        month=request.month,
        actual_demand=request.actual_demand,
        predicted_demand=request.predicted_demand,
    )
    return ActualDemandResponse(
        model_version=request.model_version,
        month=request.month,
    )


@router.get(
    "/drift-report",
    response_model=DriftReportResponse,
    dependencies=[Depends(require_permissions_models)],
    summary="Genera reporte de drift del modelo",
)
async def drift_report(
    model_version: str = Query(..., description="Versión del modelo"),
    service: MonitoringService | None = Depends(get_monitoring_service),
) -> DriftReportResponse:
    """Genera un reporte de drift para una versión de modelo dada.

    Parameters
    ----------
    model_version : str
        Identificador de la versión del modelo a analizar.
    service : MonitoringService | None
        Servicio de monitoreo inyectado.

    Returns
    -------
    DriftReportResponse
        Estructura con métricas y señales de drift del modelo.
    """
    if service is None:
        raise HTTPException(
            status_code=503,
            detail="Servicio de monitoreo no disponible: base de datos no configurada.",
        )
    report = await service.get_drift_report(model_version)
    return DriftReportResponse(**report)


@router.get(
    "/models/latest/feature-importance",
    dependencies=[Depends(require_permissions_models)],
    summary="Obtiene importancias de features del modelo actual",
)
async def feature_importance(
    service: PredictionService = Depends(get_prediction_service),
) -> dict:
    """Obtiene las importancias de features del último modelo disponible.

    Parameters
    ----------
    service : PredictionService
        Servicio de predicción inyectado para acceder al registry.

    Returns
    -------
    dict
        Diccionario con la versión del modelo y el mapa de importancias.
    """
    metadata = await service.get_latest_model()
    if not metadata:
        return {"detail": "No hay modelos disponibles"}
    importances = (metadata.metrics or {}).get("feature_importances", {})
    return {
        "model_version": metadata.version,
        "feature_importances": importances,
    }
