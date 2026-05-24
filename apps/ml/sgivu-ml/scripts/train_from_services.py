"""Entrenamiento del modelo de demanda usando los microservicios reales.

Ejerce exactamente el mismo flujo que el endpoint ``POST /v1/ml/retrain``
en producción: carga contratos desde ``sgivu-purchase-sale``, enriquece
con datos de vehículos desde ``sgivu-vehicle`` y persiste el modelo.

Uso:
    python scripts/train_from_services.py [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD]

Variables de entorno requeridas (en .env.dev de infra/compose/sgivu-docker-compose/):
    SGIVU_PURCHASE_SALE_URL      URL del servicio de compra-venta
    SGIVU_VEHICLE_URL            URL del servicio de vehículos
    SERVICE_INTERNAL_SECRET_KEY  Clave compartida de autenticación interna
    DEV_ML_DB_HOST / DEV_ML_DB_NAME / ...  Credenciales de la BD ML
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import sys
from datetime import date

import httpx

from scripts._bootstrap import build_prediction_service

from app.domain.entities import ModelMetadata
from app.domain.exceptions import DataLoadError, InsufficientHistoryError, TrainingError
from app.infrastructure.config import Settings, get_settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)

# Señal interna: el error de sklearn cuando los folds superan las muestras.
_FOLD_ERROR_MARKER = "number of folds"


def _parse_date(value: str) -> date:
    try:
        return date.fromisoformat(value)
    except ValueError:
        raise argparse.ArgumentTypeError(
            f"Fecha inválida '{value}'. Usa el formato YYYY-MM-DD."
        )


def _is_insufficient_folds_error(exc: TrainingError) -> bool:
    """Detecta si el error viene de tener menos muestras que folds de CV."""
    return _FOLD_ERROR_MARKER in str(exc).lower()


def _print_tuning_disabled_warning() -> None:
    """Imprime una advertencia muy visible sobre la desactivación del tuning."""
    # ANSI: amarillo brillante + negrita
    YELLOW = "\033[1;33m"
    RED = "\033[1;31m"
    RESET = "\033[0m"

    lines = [
        "",
        f"{RED}{'▓' * 66}{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'':^64}{RESET}{RED}▓{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'⚠  ADVERTENCIA: MODO DE ENTRENAMIENTO FORZADO':^64}{RESET}{RED}▓{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'':^64}{RESET}{RED}▓{RESET}",
        f"{RED}▓{RESET}{'':^64}{RED}▓{RESET}",
        f"{RED}▓{RESET}{'  El dataset tiene muy pocos contratos para ejecutar':<64}{RED}▓{RESET}",
        f"{RED}▓{RESET}{'  cross-validation. HYPERPARAMETER TUNING desactivado':<64}{RED}▓{RESET}",
        f"{RED}▓{RESET}{'  programáticamente para forzar el entrenamiento.':<64}{RED}▓{RESET}",
        f"{RED}▓{RESET}{'':^64}{RED}▓{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'  SOLO PARA VERIFICACIÓN EN DESARROLLO':<64}{RESET}{RED}▓{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'  En producción el volumen de datos es suficiente.':<64}{RESET}{RED}▓{RESET}",
        f"{RED}▓{RESET}{YELLOW}{'':^64}{RESET}{RED}▓{RESET}",
        f"{RED}{'▓' * 66}{RESET}",
        "",
    ]
    print("\n".join(lines), file=sys.stderr)


def _print_results(metadata: ModelMetadata) -> None:
    """Imprime el resumen del modelo entrenado."""
    metrics = metadata.metrics or {}

    print("\n" + "=" * 60)
    print("MODELO ENTRENADO EXITOSAMENTE")
    print("=" * 60)
    print(f"  Versión      : {metadata.version}")
    print(f"  Entrenado en : {metadata.trained_at}")
    print(f"  Objetivo     : {metadata.target}")
    print(
        f"  Muestras     : train={metadata.train_samples}"
        f"  test={metadata.test_samples}"
        f"  total={metadata.total_samples}"
    )

    print("\nMétricas (test set):")
    for key in ("rmse", "mae", "mape", "r2", "residual_std"):
        value = metrics.get(key)
        if value is not None:
            print(f"  {key:<14}: {value:.6f}")

    candidates = metadata.candidates or []
    if candidates:
        print("\nCandidatos evaluados:")
        for c in candidates:
            name = c.get("model", "?")
            rmse = c.get("rmse", float("nan"))
            mape = c.get("mape", float("nan"))
            print(f"  {name:<20} rmse={rmse:.4f}  mape={mape:.4f}")

    best_params = getattr(metadata, "best_params", None) or metrics.get("best_params")
    if best_params:
        print("\nMejores hiperparámetros:")
        print(f"  {json.dumps(best_params, indent=4)}")

    print("=" * 60)


async def _train_with_settings(
    settings: Settings,
    start_date: date | None,
    end_date: date | None,
) -> int:
    """Intenta entrenar con la configuración dada y gestiona el ciclo de vida."""
    service, cleanup = await build_prediction_service(settings=settings)
    try:
        metadata = await service.retrain(start_date=start_date, end_date=end_date)
    except httpx.ConnectError as exc:
        logger.error(
            "No se pudo conectar a los microservicios: %s\n"
            "Verifica que el stack dev esté corriendo (./run.sh --dev).",
            exc,
        )
        return 1
    except DataLoadError as exc:
        logger.error("Error al cargar datos: %s", exc)
        return 1
    except InsufficientHistoryError as exc:
        logger.error("Historial insuficiente: %s", exc)
        return 1
    except TrainingError as exc:
        if _is_insufficient_folds_error(exc):
            # Propaga para que el caller reintente sin tuning.
            raise
        logger.error("Error de entrenamiento: %s", exc)
        return 1
    finally:
        await cleanup()

    _print_results(metadata)
    return 0


async def run(start_date: date | None, end_date: date | None) -> int:
    """Orquesta el entrenamiento con reintentos automáticos sin tuning."""
    logger.info(
        "Iniciando entrenamiento (start_date=%s, end_date=%s)…",
        start_date,
        end_date,
    )
    settings = get_settings()

    try:
        return await _train_with_settings(settings, start_date, end_date)
    except TrainingError:
        pass

    # El tuning falló por muestras insuficientes: reintenta sin él.
    _print_tuning_disabled_warning()
    no_tuning_settings = settings.model_copy(
        update={"enable_hyperparameter_tuning": False}
    )
    return await _train_with_settings(no_tuning_settings, start_date, end_date)


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Entrena el modelo de demanda usando los microservicios reales "
            "(sgivu-purchase-sale y sgivu-vehicle). "
            "Equivalente al endpoint POST /v1/ml/retrain en producción."
        )
    )
    parser.add_argument(
        "--start-date",
        type=_parse_date,
        default=None,
        metavar="YYYY-MM-DD",
        help="Fecha de inicio para filtrar contratos (inclusive).",
    )
    parser.add_argument(
        "--end-date",
        type=_parse_date,
        default=None,
        metavar="YYYY-MM-DD",
        help="Fecha de fin para filtrar contratos (inclusive).",
    )
    args = parser.parse_args()
    code = asyncio.run(run(args.start_date, args.end_date))
    sys.exit(code)


if __name__ == "__main__":
    main()
