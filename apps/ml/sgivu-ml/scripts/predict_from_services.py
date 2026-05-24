"""Predicción de demanda usando los microservicios reales.

Ejerce exactamente el mismo flujo que el endpoint ``POST /v1/ml/predict``
en producción: carga el último modelo entrenado, obtiene el historial del
segmento y genera el pronóstico con intervalos de confianza.

Uso:
    python scripts/predict_from_services.py \\
        --vehicle-type MOTORCYCLE --brand Yamaha --model MT-03 --line MT \\
        [--horizon 6] [--confidence 0.95] [--plot /tmp/forecast.png]

Variables de entorno requeridas (en .env o como vars de shell):
    SGIVU_PURCHASE_SALE_URL  URL del servicio de compra-venta (ej: http://localhost:8084)
    SGIVU_VEHICLE_URL        URL del servicio de vehículos   (ej: http://localhost:8083)
    SERVICE_INTERNAL_SECRET_KEY  Clave compartida de autenticación interna
    DEV_ML_DB_HOST / DEV_ML_DB_NAME / ...  Credenciales de la BD (si hay BD configurada)
"""

from __future__ import annotations

import argparse
import asyncio
import logging
import sys
from pathlib import Path
from typing import Any, Dict

import httpx
import pandas as pd

from scripts._bootstrap import build_prediction_service

from app.domain.exceptions import (
    DataLoadError,
    MissingVehicleLineError,
    ModelNotTrainedError,
    SegmentNotFoundError,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)


def _history_to_dataframe(history_points: list) -> pd.DataFrame:
    """Convierte la lista de ``HistoricalPoint`` en un DataFrame para el gráfico."""
    return pd.DataFrame(
        [
            {
                "event_month": pd.Timestamp(p.month),
                "sales_count": p.sales_count,
            }
            for p in history_points
        ]
    )


def _forecast_to_dicts(predictions: list) -> list[Dict[str, Any]]:
    """Convierte ``ForecastPoint`` Pydantic a dicts para el gráfico."""
    return [p.model_dump() for p in predictions]


def _print_result(result: Any, confidence: float, horizon: int) -> None:
    """Imprime en consola el resultado completo de la predicción."""
    metrics = result.metrics or {}

    print("\n" + "=" * 60)
    print("PREDICCIÓN DE DEMANDA")
    print("=" * 60)
    print(f"  Segmento     : {result.segment}")
    print(f"  Versión      : {result.model_version}")
    print(f"  Entrenado en : {result.trained_at}")
    print(f"  Confianza    : {confidence:.0%}")

    print("\nMétricas del modelo:")
    for key in ("rmse", "mae", "mape", "r2", "residual_std"):
        value = metrics.get(key)
        if value is not None:
            print(f"  {key:<14}: {value:.6f}")

    if result.history:
        recent_history = result.history[-12:]
        print(f"\nHistorial reciente ({len(recent_history)} meses):")
        for point in recent_history:
            print(f"  {point.month}   ventas={point.sales_count:.0f}")

    print(f"\nPronóstico ({horizon} meses):")
    print(f"  {'Mes':<12}  {'Demanda':>8}  {'IC inf':>8}  {'IC sup':>8}")
    print(f"  {'-'*12}  {'-'*8}  {'-'*8}  {'-'*8}")
    for point in result.predictions:
        print(
            f"  {point.month:<12}  {point.demand:>8.2f}  "
            f"{point.lower_ci:>8.2f}  {point.upper_ci:>8.2f}"
        )
    print("=" * 60)


def _save_plot(
    result: Any,
    filters: Dict[str, str],
    horizon: int,
    plot_path: Path,
) -> None:
    """Genera y guarda el gráfico PNG del pronóstico."""
    try:
        from tests.csv_offline_demo import plot_forecast

        history_df = _history_to_dataframe(result.history)
        forecast_dicts = _forecast_to_dicts(result.predictions)
        plot_forecast(history_df, forecast_dicts, filters, horizon, plot_path)
        print(f"\nGráfico guardado en: {plot_path.resolve()}")
    except ImportError:
        logger.warning(
            "No se pudo importar plot_forecast desde tests.csv_offline_demo. "
            "Instala matplotlib para generar gráficos."
        )


async def run(
    filters: Dict[str, str],
    horizon: int,
    confidence: float,
    plot_path: Path | None,
) -> int:
    service, cleanup = await build_prediction_service()
    try:
        logger.info(
            "Prediciendo demanda para segmento: %s (horizonte=%d)…", filters, horizon
        )
        result = await service.predict_with_history(
            filters=filters,
            horizon=horizon,
            confidence=confidence,
        )
    except httpx.ConnectError as exc:
        logger.error(
            "No se pudo conectar a los microservicios: %s\n"
            "Verifica que el stack dev esté corriendo y que .env tenga:\n"
            "  SGIVU_PURCHASE_SALE_URL=http://localhost:8084\n"
            "  SGIVU_VEHICLE_URL=http://localhost:8083",
            exc,
        )
        return 1
    except ModelNotTrainedError as exc:
        logger.error("Sin modelo entrenado: %s", exc)
        return 1
    except SegmentNotFoundError as exc:
        logger.error("Segmento sin historial: %s", exc)
        return 1
    except MissingVehicleLineError as exc:
        logger.error("Línea de vehículo faltante: %s", exc)
        return 1
    except DataLoadError as exc:
        logger.error("Error al cargar datos: %s", exc)
        return 1
    finally:
        await cleanup()

    _print_result(result, confidence, horizon)

    if plot_path:
        _save_plot(result, filters, horizon, plot_path)

    return 0


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Predice la demanda de un segmento de vehículo usando los microservicios "
            "reales. Equivalente al endpoint POST /v1/ml/predict en producción."
        )
    )
    parser.add_argument(
        "--vehicle-type",
        required=True,
        metavar="TIPO",
        help="Tipo de vehículo: CAR o MOTORCYCLE.",
    )
    parser.add_argument(
        "--brand",
        required=True,
        metavar="MARCA",
        help="Marca del vehículo (ej: Yamaha).",
    )
    parser.add_argument(
        "--model",
        required=True,
        metavar="MODELO",
        help="Modelo del vehículo (ej: MT-03).",
    )
    parser.add_argument(
        "--line",
        required=True,
        metavar="LINEA",
        help="Línea o submodelo del vehículo (ej: MT). Obligatoria.",
    )
    parser.add_argument(
        "--horizon",
        type=int,
        default=6,
        metavar="MESES",
        help="Número de meses a pronosticar (default: 6).",
    )
    parser.add_argument(
        "--confidence",
        type=float,
        default=0.95,
        metavar="NIVEL",
        help="Nivel de confianza para los intervalos (default: 0.95).",
    )
    parser.add_argument(
        "--plot",
        type=Path,
        default=None,
        metavar="RUTA",
        help="Ruta para guardar el gráfico PNG del pronóstico (opcional).",
    )
    args = parser.parse_args()

    filters: Dict[str, str] = {
        "vehicle_type": args.vehicle_type,
        "brand": args.brand,
        "model": args.model,
        "line": args.line,
    }
    code = asyncio.run(
        run(
            filters=filters,
            horizon=args.horizon,
            confidence=args.confidence,
            plot_path=args.plot,
        )
    )
    sys.exit(code)


if __name__ == "__main__":
    main()
