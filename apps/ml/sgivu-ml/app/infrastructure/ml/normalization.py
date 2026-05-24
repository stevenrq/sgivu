"""Funciones de normalización de texto para etiquetas de vehículos.

Módulo sin estado ni efectos secundarios: pura transformación de datos.
"""

from __future__ import annotations

import re
import unicodedata
from typing import Any, Tuple

UNKNOWN_TOKEN = "UNKNOWN"
_UNKNOWN_ALIASES = {
    "",
    "UNKNOWN",
    "ND",
    "N D",
    "NA",
    "N A",
    "NULL",
    "NONE",
    "NO DISPONIBLE",
    "SIN DATO",
}


def _strip_accents(text: str) -> str:
    """Elimina diacríticos de un texto Unicode."""
    normalized = unicodedata.normalize("NFKD", text)
    return "".join(ch for ch in normalized if not unicodedata.combining(ch))


def canonicalize_label(value: Any) -> str:
    """Normaliza una etiqueta: mayúsculas, sin acentos, sin caracteres especiales."""
    if value is None:
        return ""
    text = str(value).upper().strip()
    text = _strip_accents(text)
    text = re.sub(r"[^A-Z0-9]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def normalize_unknown_alias(value: Any, fallback: str = UNKNOWN_TOKEN) -> str:
    """Normaliza aliases de faltantes al token canónico de fallback."""
    if value is None:
        return fallback

    raw = str(value).strip()
    if not raw:
        return fallback

    canonical = canonicalize_label(raw)
    if canonical in _UNKNOWN_ALIASES:
        return fallback

    return raw


def canonicalize_label_or_unknown(value: Any, fallback: str = UNKNOWN_TOKEN) -> str:
    """Normaliza etiqueta y aplica fallback canónico cuando el valor es faltante."""
    normalized = normalize_unknown_alias(value, fallback)
    if normalized == fallback:
        return fallback

    canonical = canonicalize_label(normalized)
    return canonical if canonical else fallback


def canonicalize_brand_model(brand: Any, model: Any) -> Tuple[str, str]:
    """Normaliza marca y modelo como par consistente."""
    normalized_brand = canonicalize_label(brand)
    normalized_model = canonicalize_label(model)
    return normalized_brand, normalized_model
