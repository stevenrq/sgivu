"""Prueba offline del modelo de demanda con un CSV local."""

from __future__ import annotations

import argparse
import asyncio
import logging
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.ticker import MaxNLocator

from app.application.services.prediction_service import PredictionService
from app.application.services.training_service import TrainingService
from app.domain.ports.feature_repository import FeatureRepositoryPort
from app.domain.ports.model_registry import ModelRegistryPort
from app.domain.ports.prediction_repository import PredictionRepositoryPort
from app.infrastructure.config import Settings
from app.infrastructure.ml.feature_engineering import FeatureEngineering
from app.infrastructure.ml.model_training import ModelTrainer
from app.infrastructure.ml.normalization import (
    canonicalize_brand_model,
    canonicalize_label,
    canonicalize_label_or_unknown,
    normalize_unknown_alias,
)
from app.infrastructure.persistence.database import (
    close_engine,
    database_enabled,
    get_session_factory,
    init_engine,
)
from app.infrastructure.persistence.db_model_registry import DatabaseModelRegistry
from app.infrastructure.persistence.feature_repository import FeatureRepository
from app.infrastructure.persistence.fs_model_registry import FileSystemModelRegistry
from app.infrastructure.persistence.prediction_repository import PredictionRepository

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
logger = logging.getLogger(__name__)


SPANISH_CONTRACT_TYPE = {"compra": "PURCHASE", "venta": "SALE"}
SPANISH_STATUS = {
    "activa": "ACTIVE",
    "pendiente": "PENDING",
    "completado": "COMPLETED",
    "completada": "COMPLETED",
    "cancelado": "CANCELED",
    "cancelada": "CANCELED",
}
SPANISH_PAYMENT = {
    "efectivo": "CASH",
    "transferencia bancaria": "BANK_TRANSFER",
    "transferencia": "BANK_TRANSFER",
    "consignacion": "BANK_DEPOSIT",
    "consignación bancaria": "BANK_DEPOSIT",
    "cheque de gerencia": "CASHIERS_CHECK",
    "mixto": "MIXED",
    "financiacion": "FINANCING",
    "financiación": "FINANCING",
    "billetera digital": "DIGITAL_WALLET",
    "permuta": "TRADE_IN",
    "cuotas": "INSTALLMENT_PAYMENT",
}
SPANISH_VEHICLE_TYPE = {
    "automovil": "CAR",
    "automóvil": "CAR",
    "motocicleta": "MOTORCYCLE",
}
VEHICLE_TYPE_DISPLAY = {"CAR": "Auto", "MOTORCYCLE": "Motocicleta"}

SPANISH_VEHICLE_STATUS = {
    "disponible": "AVAILABLE",
    "vendido": "SOLD",
    "en mantenimiento": "IN_MAINTENANCE",
    "en reparacion": "IN_REPAIR",
    "en reparación": "IN_REPAIR",
    "en uso": "IN_USE",
    "inactivo": "INACTIVE",
    "available": "AVAILABLE",
    "sold": "SOLD",
    "in_maintenance": "IN_MAINTENANCE",
    "in_repair": "IN_REPAIR",
    "in_use": "IN_USE",
    "inactive": "INACTIVE",
    "unknown": "UNKNOWN",
}

DT64 = "datetime64[ns]"


def _parse_datetime(value: str) -> str:
    try:
        dt = datetime.strptime(value.strip(), "%d/%m/%Y %H:%M")
        return dt.isoformat()
    except Exception:
        return value


def _normalize(value: Any) -> str:
    return str(value).strip().upper() if value is not None else ""


def load_csv(path: Path) -> pd.DataFrame:
    df = pd.read_csv(path)

    # Normaliza nombres quitando comillas.
    df.columns = [c.strip().strip('"') for c in df.columns]

    # Generar IDs de vehiculo a partir de la placa.
    vehicle_id_map: Dict[str, int] = {}
    next_id = 1

    records = []
    for _, row in df.iterrows():
        plate = str(row.get("Placa del vehículo", "")).strip().upper()
        if plate and plate not in vehicle_id_map:
            vehicle_id_map[plate] = next_id
            next_id += 1

        vehicle_id = vehicle_id_map.get(plate, None)
        contract_type_raw = _normalize(row.get("Tipo de contrato"))
        contract_status_raw = _normalize(row.get("Estado del contrato"))
        vehicle_type_raw = _normalize(row.get("Tipo de vehículo"))
        vehicle_status_raw = _normalize(row.get("Estado del vehículo"))
        payment_raw = _normalize(row.get("Método de pago"))
        raw_line = row.get("Línea del vehículo") or row.get("Linea del vehiculo")
        line_value = canonicalize_label_or_unknown(raw_line)
        vehicle_status_value = SPANISH_VEHICLE_STATUS.get(
            vehicle_status_raw.lower(),
            vehicle_status_raw,
        )
        vehicle_status_value = normalize_unknown_alias(vehicle_status_value)

        records.append(
            {
                "contract_id": len(records) + 1,
                "contract_type": SPANISH_CONTRACT_TYPE.get(
                    contract_type_raw.lower(), contract_type_raw
                ),
                "contract_status": SPANISH_STATUS.get(
                    contract_status_raw.lower(), contract_status_raw
                ),
                "client_id": 0,
                "user_id": 0,
                "vehicle_id": vehicle_id,
                "purchase_price": float(
                    str(row.get("Precio de compra", 0)).replace(",", "")
                ),
                "sale_price": float(
                    str(row.get("Precio de venta", 0)).replace(",", "")
                ),
                "payment_method": SPANISH_PAYMENT.get(payment_raw.lower(), payment_raw),
                "observations": row.get("Observaciones", ""),
                "created_at": _parse_datetime(str(row.get("Fecha de creación", ""))),
                "updated_at": _parse_datetime(str(row.get("Última actualización", ""))),
                "vehicle_type": SPANISH_VEHICLE_TYPE.get(
                    vehicle_type_raw.lower(), vehicle_type_raw
                ),
                "brand": _normalize(row.get("Marca del vehículo")),
                "model": _normalize(row.get("Modelo del vehículo")),
                "line": line_value,
                "plate": plate,
                "year": None,
                "mileage": None,
                "vehicle_status": vehicle_status_value,
            }
        )

    return pd.DataFrame(records)


def plot_forecast(
    history: pd.DataFrame,
    forecast: list[Dict[str, Any]],
    filters: Dict[str, str],
    horizon: int,
    output_path: Path,
) -> None:
    hist = history.copy()
    if not hist.empty:
        ref_month = hist["event_month"].max()
        cutoff = (ref_month - pd.DateOffset(months=11)).to_period("M").to_timestamp()
        hist = hist[
            (hist["event_month"] >= cutoff) & (hist["event_month"] <= ref_month)
        ]
    hist = (
        hist.groupby("event_month")["sales_count"]
        .sum()
        .reset_index()
        .sort_values("event_month")
    )
    months_hist = pd.to_datetime(hist["event_month"])
    y_hist = hist["sales_count"]

    months = [datetime.fromisoformat(item["month"]) for item in forecast]
    y = [item["demand"] for item in forecast]
    lower = [item["lower_ci"] for item in forecast]
    upper = [item["upper_ci"] for item in forecast]

    plt.figure(figsize=(11, 5))
    ax = plt.gca()
    plt.gcf().patch.set_facecolor("#f7f9fb")
    ax.set_facecolor("#ffffff")

    # Conversión a fechas python para formatear etiquetas
    months_hist_dt = (
        [dt.to_pydatetime() for dt in months_hist] if not hist.empty else []
    )
    months_dt = list(months)
    months_dt_np = np.array(months_dt, dtype=DT64)
    months_hist_np = (
        np.array(months_hist_dt, dtype=DT64)
        if months_hist_dt
        else np.array([], dtype=DT64)
    )

    # Pronóstico (azul)
    plt.plot(
        months_dt_np,
        y,
        marker="o",
        color="#1d65d8",
        linewidth=2.0,
        label="Demanda Predicha",
    )
    plt.fill_between(
        months_dt_np, lower, upper, color="#1d65d8", alpha=0.15, label="IC 95%"
    )

    # Historial (gris punteado)
    if not hist.empty:
        plt.plot(
            months_hist_np,
            y_hist,
            marker="o",
            color="#6b7280",
            linestyle="-",
            linewidth=1.5,
            label="Ventas Históricas",
        )

    vehicle_type_raw = (filters.get("vehicle_type", "") or "").strip()
    vehicle_type = VEHICLE_TYPE_DISPLAY.get(vehicle_type_raw, vehicle_type_raw)
    brand = (filters.get("brand", "") or "").strip()
    model = (filters.get("model", "") or "").strip()

    parts = [p for p in [vehicle_type, brand, model] if p]
    title = (
        f"Predicción de Demanda (Próximos {horizon} Meses) | {' '.join(parts)}".strip()
    )
    plt.title(title)
    plt.xlabel("Mes")
    plt.ylabel("Unidades")
    all_months = sorted(set(months_dt + months_hist_dt))
    all_months_np = np.array(all_months, dtype=DT64)
    month_names = [
        "Ene",
        "Feb",
        "Mar",
        "Abr",
        "May",
        "Jun",
        "Jul",
        "Ago",
        "Sep",
        "Oct",
        "Nov",
        "Dic",
    ]
    labels = [f"{month_names[m.month-1]} {m.year}" for m in all_months]
    plt.xticks(all_months_np, labels, rotation=45)
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    all_y_values = (
        list(y_hist.values if hasattr(y_hist, "values") else y_hist) + y + upper + lower
    )
    if all_y_values:
        max_y = max(all_y_values)
        min_y = min(all_y_values)
    else:
        max_y, min_y = 1.0, 0.0
    # Evitar que los negativos se recorten: extender el eje si hay valores < 0.
    max_y = max(max_y, 1.0)
    min_y = min(min_y, 0.0)
    span = max_y - min_y
    padding = span * 0.2 if span > 0 else 1.0
    ax.set_ylim(min_y - padding, max_y + padding)
    plt.grid(True, axis="y", linestyle="--", alpha=0.3)
    for spine in ["top", "right"]:
        ax.spines[spine].set_visible(False)
    plt.figtext(
        0.01,
        0.01,
        f"Horizonte: {horizon} meses",
        fontsize=9,
    )
    plt.legend()
    plt.tight_layout()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(output_path)
    plt.close()
    logger.info("Forecast chart saved at: %s", output_path)


def _print_model_report(metadata: Any) -> None:
    """Imprime un resumen legible de la calidad del modelo.

    Muestra métricas globales, desviaciones residuales por horizonte/segmento
    y las importancias de features más relevantes. Evita volcar JSON crudo
    en la consola para facilitar la lectura humana.
    """
    metrics = getattr(metadata, "metrics", None) or {}
    if not metrics:
        logger.info("No metrics available in metadata.")
        return

    rmse = metrics.get("rmse")
    mae = metrics.get("mae")
    mape = metrics.get("mape")
    r2 = metrics.get("r2")
    residual_std = metrics.get("residual_std")
    feature_importances = metrics.get("feature_importances", {}) or {}
    segment_residuals = metrics.get("segment_residuals", {}) or {}
    horizon_residuals = metrics.get("horizon_residuals", {}) or {}

    print("\n=== Model Report ===")
    print(f"Version : {getattr(metadata, 'version', 'unknown')}")
    if getattr(metadata, "trained_at", None):
        print(f"Trained at: {getattr(metadata, 'trained_at')}")

    print("\n-- Muestras de datos --")
    print(f"  Train samples: {getattr(metadata, 'train_samples', 'n/a')}")
    print(f"  Test  samples: {getattr(metadata, 'test_samples', 'n/a')}")
    print(f"  Total samples: {getattr(metadata, 'total_samples', 'n/a')}")

    print("\n-- Métricas globales --")
    if rmse is not None:
        print(f"  RMSE : {rmse:.3f}")
    if mae is not None:
        print(f"  MAE  : {mae:.3f}")
    if mape is not None:
        # Mostrar tanto el valor como un porcentaje cuando tiene sentido
        try:
            mape_val = float(mape)
            if mape_val <= 1:
                print(f"  MAPE : {mape_val:.3f} ({mape_val*100:.1f}%)")
            else:
                print(f"  MAPE : {mape_val:.3f}")
        except Exception:
            print(f"  MAPE : {mape}")
    if r2 is not None:
        print(f"  R²   : {r2:.3f}")
    if residual_std is not None:
        print(f"  Residual std: {residual_std:.3f}")

    if horizon_residuals:
        print("\n-- Residuales por horizonte (desv. estándar) --")
        for step in sorted(horizon_residuals.keys()):
            print(f"  h={step}: {horizon_residuals[step]:.3f}")

    if segment_residuals:
        print("\n-- Residuales por segmento (8 con mayor residual) --")
        try:
            items = sorted(
                segment_residuals.items(), key=lambda kv: kv[1], reverse=True
            )
            for key, val in items[:8]:
                print(f"  {key}: {val:.3f}")
        except Exception:
            print("  (could not format segment residuals)")

    if feature_importances:
        print("\n-- Importancia de características (10 principales) --")
        try:
            items = sorted(
                feature_importances.items(), key=lambda kv: kv[1], reverse=True
            )
            total = sum(items_val for _, items_val in items) or 1.0
            for name, val in items[:10]:
                pct = 100.0 * float(val) / total
                print(f"  {name:20s}: {val:.4f} ({pct:.1f}%)")
        except Exception:
            print("  (could not format feature importances)")


async def run(  # noqa: C901
    csv_path: Path,
    horizon: int,
    model_dir: Path,
    filters: Dict[str, str],
    plot_path: Optional[Path] = None,
) -> None:
    settings = Settings(model_dir=str(model_dir), min_history_months=1)
    feature_repository: FeatureRepositoryPort | None = None
    prediction_repository: PredictionRepositoryPort | None = None
    registry: ModelRegistryPort

    if database_enabled(settings):
        await init_engine(settings)
        factory = get_session_factory()
        assert factory is not None
        registry = DatabaseModelRegistry(factory, settings.model_name)
        feature_repository = FeatureRepository(factory)
        prediction_repository = PredictionRepository(factory)
    else:
        registry = FileSystemModelRegistry(settings)

    feature_engineering = FeatureEngineering(settings)
    model_trainer = ModelTrainer(settings)

    trainer = TrainingService(
        registry=registry,
        feature_engineering=feature_engineering,
        model_trainer=model_trainer,
        feature_repository=feature_repository,
        settings=settings,
    )
    service = PredictionService(
        registry=registry,
        feature_engineering=feature_engineering,
        training_service=trainer,
        transaction_loader=None,
        feature_repository=feature_repository,
        prediction_repository=prediction_repository,
        settings=settings,
    )

    canonical_type = canonicalize_label(filters["vehicle_type"])
    canonical_brand, canonical_model = canonicalize_brand_model(
        filters["brand"], filters["model"]
    )
    canonical_line = canonicalize_label(filters.get("line", ""))
    if not canonical_type or not canonical_brand or not canonical_model:
        logger.error("Missing required parameters: vehicle_type, brand and model.")
        return
    if not canonical_line:
        logger.error("Vehicle line is required and cannot be empty.")
        return
    if canonical_line == "UNKNOWN":
        logger.error("Vehicle line cannot be UNKNOWN.")
        return

    df = load_csv(csv_path)
    if df.empty:
        logger.error("CSV has no rows.")
        return

    logger.info("Rows loaded: %s", len(df))
    try:
        trained_metadata = await trainer.train(df)
    except Exception as exc:
        logger.error("Failed to train model: %s", exc)
        return
    # Metrics/reporting will be displayed in a human-friendly format
    # after the model is persisted and reloaded from the registry.

    feature_df = feature_engineering.build_feature_table(df)

    base_mask = (
        (feature_df["vehicle_type"] == canonical_type)
        & (feature_df["brand"] == canonical_brand)
        & (feature_df["model"] == canonical_model)
    )
    mask_with_line = base_mask & (feature_df["line"] == canonical_line)
    history = feature_df[mask_with_line].copy()
    if history.empty:
        logger.error(
            "No history found for requested filters: %s. "
            "Verify that brand/model/line exist in the CSV.",
            filters,
        )
        return

    model, latest_metadata = await registry.load_latest()
    # Mostrar un informe legible de la calidad del modelo (reloaded metadata)
    _print_model_report(latest_metadata)
    forecast = service._forecast(
        model=model,
        metadata=latest_metadata,
        history=history,
        horizon=horizon,
        confidence=0.95,
    )

    print("\nPronostico:")
    for item in forecast:
        print(
            f"{item['month']}: demanda={item['demand']:.2f}, "
            f"IC95% [{item['lower_ci']:.2f}, {item['upper_ci']:.2f}]"
        )

    if prediction_repository:
        history_payload = [
            {
                "month": ts.date().isoformat(),
                "sales_count": float(sc),
            }
            for ts, sc in zip(history["event_month"], history["sales_count"])
        ]
        request_payload = {
            "vehicle_type": filters["vehicle_type"],
            "brand": filters["brand"],
            "model": filters["model"],
            "line": filters["line"],
            "horizon_months": horizon,
            "confidence": 0.95,
        }
        response_payload = {
            "predictions": forecast,
            "history": history_payload,
            "segment": {
                "vehicle_type": canonical_type,
                "brand": canonical_brand,
                "model": canonical_model,
                "line": canonical_line,
            },
            "model_version": latest_metadata.version,
            "trained_at": latest_metadata.trained_at,
            "metrics": latest_metadata.metrics,
        }
        await prediction_repository.save_prediction(
            model_version=latest_metadata.version,
            request_payload=request_payload,
            response_payload=response_payload,
            segment=response_payload["segment"],
            horizon=horizon,
            confidence=0.95,
            with_history=True,
        )

    if plot_path:
        plot_forecast(history, forecast, filters, horizon, plot_path)

    await close_engine()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Prueba offline del modelo de demanda con un CSV local."
    )
    parser.add_argument(
        "--csv", required=True, type=Path, help="Ruta al archivo CSV de compras/ventas."
    )
    parser.add_argument(
        "--horizon", type=int, default=6, help="Meses a predecir (default: 6)."
    )
    parser.add_argument(
        "--model-dir",
        type=Path,
        default=Path("models_offline"),
        help="Directorio para artefactos.",
    )
    parser.add_argument("--vehicle-type", required=True, help="CAR o MOTORCYCLE.")
    parser.add_argument("--brand", required=True, help="Marca (ej: YAMAHA).")
    parser.add_argument("--model", required=True, help="Modelo (ej: MT-03).")
    parser.add_argument("--line", required=True, help="Linea/submodelo (obligatoria).")
    parser.add_argument(
        "--plot",
        required=False,
        default=None,
        type=Path,
        help="Ruta para guardar la grafica PNG del pronostico (opcional).",
    )

    args = parser.parse_args()
    asyncio.run(
        run(
            csv_path=args.csv,
            horizon=args.horizon,
            model_dir=args.model_dir,
            filters={
                "vehicle_type": args.vehicle_type,
                "brand": args.brand,
                "model": args.model,
                "line": args.line,
            },
            plot_path=args.plot,
        )
    )
