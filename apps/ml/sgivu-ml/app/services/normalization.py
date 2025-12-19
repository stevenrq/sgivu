from __future__ import annotations

import re
import unicodedata
from typing import Any, Tuple


def _strip_accents(text: str) -> str:
    """Quita acentos/diacríticos para evitar variaciones innecesarias."""
    normalized = unicodedata.normalize("NFKD", text)
    return "".join(ch for ch in normalized if not unicodedata.combining(ch))


def canonicalize_label(value: Any) -> str:
    """Normaliza etiquetas de entrada (marca, modelo, línea) a un formato estable."""
    if value is None:
        return ""
    text = str(value).upper().strip()
    text = _strip_accents(text)
    text = re.sub(r"[^A-Z0-9]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def canonicalize_brand_model(brand: Any, model: Any) -> Tuple[str, str]:
    """Aplica normalización a marca/modelo para consolidar segmentos."""
    normalized_brand = canonicalize_label(brand)
    normalized_model = canonicalize_label(model)
    return normalized_brand, normalized_model
