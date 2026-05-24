"""Extracción de importancias de features desde modelos entrenados.

Función pura que examina el pipeline de sklearn y extrae las
importancias de features nativas del estimador (RF o XGB).
"""

from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


def extract_importance(pipeline: Any, feature_names: list[str]) -> dict[str, float]:
    """Extrae importancias de features del modelo dentro del pipeline.

    Soporta `RandomForestRegressor`, `XGBRegressor` y cualquier estimador
    que exponga el atributo ``feature_importances_``. La función mapea
    las características transformadas por el `ColumnTransformer` a los
    nombres originales y agrega puntajes por feature.

    Parameters
    ----------
    pipeline : Any
        Pipeline de sklearn que contiene pasos `preprocess` y `model`.
    feature_names : list[str]
        Lista de nombres de features originales que se usaron para entrenar.

    Returns
    -------
    dict[str, float]
        Diccionario `{feature_name: importance}` normalizado (suma 1.0),
        ordenado de mayor a menor. Retorna dict vacío si no se pueden extraer
        importancias.
    """
    try:
        model = (
            pipeline.named_steps.get("model")
            if hasattr(pipeline, "named_steps")
            else None
        )
        if model is None:
            return {}

        importances = getattr(model, "feature_importances_", None)
        if importances is None:
            return {}

        preprocessor = pipeline.named_steps.get("preprocess")
        if preprocessor is None:
            return {}

        transformed_names = _get_transformed_names(preprocessor)

        if len(importances) != len(transformed_names):
            logger.warning(
                "Feature count mismatch: %d importances vs %d names",
                len(importances),
                len(transformed_names),
            )
            return {}

        raw_importance: dict[str, float] = {}
        for name, score in zip(transformed_names, importances):
            base_name = _map_to_original(name, feature_names)
            raw_importance[base_name] = raw_importance.get(base_name, 0.0) + float(
                score
            )

        total = sum(raw_importance.values())
        if total > 0:
            raw_importance = {k: v / total for k, v in raw_importance.items()}

        return dict(sorted(raw_importance.items(), key=lambda x: x[1], reverse=True))

    except Exception as exc:
        logger.warning("Failed to extract feature importances: %s", exc)
        return {}


def _get_transformed_names(preprocessor: Any) -> list[str]:
    """Obtiene los nombres de features transformados del ColumnTransformer."""
    try:
        return list(preprocessor.get_feature_names_out())
    except Exception:
        names: list[str] = []
        for name, transformer, columns in preprocessor.transformers_:
            if name == "remainder":
                continue
            if hasattr(transformer, "get_feature_names_out"):
                names.extend(transformer.get_feature_names_out(columns))
            else:
                names.extend(columns if isinstance(columns, list) else [columns])
        return names


def _map_to_original(transformed_name: str, original_names: list[str]) -> str:
    """Mapea un nombre de feature transformado al nombre original.

    Los nombres del ColumnTransformer tienen formato
    ``categorical__vehicle_type_CAR`` o ``numeric__lag_1``.
    """
    parts = transformed_name.split("__", 1)
    feature_part = parts[-1] if len(parts) > 1 else transformed_name

    for orig in original_names:
        if feature_part == orig or feature_part.startswith(orig + "_"):
            return orig

    return feature_part
