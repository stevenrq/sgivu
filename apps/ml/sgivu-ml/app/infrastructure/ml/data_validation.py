"""Validación y limpieza de datos para el pipeline de ML.

Funciones puras sin I/O ni efectos secundarios.  Validan la calidad
de los datos de entrada y aplican clipping de outliers.
"""

from __future__ import annotations

import logging

import pandas as pd

logger = logging.getLogger(__name__)

REQUIRED_COLUMNS: list[str] = [
    "vehicle_type",
    "brand",
    "model",
    "line",
    "contract_type",
    "sale_price",
    "purchase_price",
    "vehicle_id",
    "created_at",
]


def validate_raw_data(df: pd.DataFrame) -> tuple[pd.DataFrame, list[str]]:
    """Valida que el DataFrame contenga las columnas requeridas.

    Parameters
    ----------
    df : pd.DataFrame
        DataFrame bruto con las transacciones a validar.

    Returns
    -------
    tuple[pd.DataFrame, list[str]]
        Tupla con el DataFrame (posiblemente sin cambios) y una lista de advertencias.

    Raises
    ------
    ValueError
        Si alguna columna crítica del conjunto `REQUIRED_COLUMNS` está ausente.
    """
    warnings: list[str] = []
    missing = [col for col in REQUIRED_COLUMNS if col not in df.columns]
    if missing:
        raise ValueError(
            f"Columnas requeridas ausentes en los datos: {', '.join(missing)}"
        )

    for col in ["vehicle_type", "brand", "model", "line"]:
        null_count = (
            df[col].isna().sum() + (df[col].astype(str).str.strip() == "").sum()
        )
        if null_count > 0:
            pct = null_count / len(df) * 100
            warnings.append(
                f"Columna '{col}' tiene {null_count} valores vacíos ({pct:.1f}%)"
            )

    return df, warnings


def clip_outliers(
    df: pd.DataFrame, column: str, iqr_multiplier: float = 3.0
) -> pd.DataFrame:
    """Aplica clipping IQR a una columna numérica.

    Valores fuera de [Q1 - k*IQR, Q3 + k*IQR] se recortan al límite.
    """
    if column not in df.columns or df[column].isna().all():
        return df

    q1 = df[column].quantile(0.25)
    q3 = df[column].quantile(0.75)
    iqr = q3 - q1

    if iqr == 0:
        return df

    lower = q1 - iqr_multiplier * iqr
    upper = q3 + iqr_multiplier * iqr
    clipped = df.copy()
    clipped[column] = clipped[column].clip(lower=lower, upper=upper)

    n_clipped = (df[column] < lower).sum() + (df[column] > upper).sum()
    if n_clipped > 0:
        logger.info(
            "Clipped %d outliers in column '%s' to [%.2f, %.2f]",
            n_clipped,
            column,
            lower,
            upper,
        )

    return clipped
