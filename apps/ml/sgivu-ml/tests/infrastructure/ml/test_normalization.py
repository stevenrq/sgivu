"""Tests para utilidades de normalización de etiquetas."""

from __future__ import annotations

import pytest

from app.infrastructure.ml.normalization import (
    UNKNOWN_TOKEN,
    canonicalize_label_or_unknown,
    normalize_unknown_alias,
)


class TestNormalizeUnknownAlias:
    """normalize_unknown_alias()"""

    @pytest.mark.parametrize(
        "value",
        [
            None,
            "",
            "   ",
            "N/D",
            "nd",
            "N d",
            "unknown",
            "NULL",
            "No disponible",
            "Sin dato",
        ],
    )
    def test_returns_unknown_when_alias_is_missing(self, value: object) -> None:
        """Debe mapear aliases de faltantes al token UNKNOWN."""
        assert normalize_unknown_alias(value) == UNKNOWN_TOKEN

    @pytest.mark.parametrize(
        ("value", "expected"),
        [
            ("Disponible", "Disponible"),
            ("IN_REPAIR", "IN_REPAIR"),
            ("MT-03", "MT-03"),
        ],
    )
    def test_preserves_value_when_alias_is_not_missing(
        self, value: str, expected: str
    ) -> None:
        """Debe preservar valores no faltantes sin alterarlos."""
        assert normalize_unknown_alias(value) == expected


class TestCanonicalizeLabelOrUnknown:
    """canonicalize_label_or_unknown()"""

    @pytest.mark.parametrize(
        "value",
        [None, "", "N/D", "ND", "unknown", "No disponible"],
    )
    def test_returns_unknown_when_alias_is_detected(self, value: object) -> None:
        """Debe devolver UNKNOWN cuando el valor representa faltante."""
        assert canonicalize_label_or_unknown(value) == UNKNOWN_TOKEN

    @pytest.mark.parametrize(
        ("value", "expected"),
        [
            ("Mt-03", "MT 03"),
            ("En reparación", "EN REPARACION"),
            ("Corolla XEi", "COROLLA XEI"),
        ],
    )
    def test_canonicalizes_value_when_input_is_valid(
        self, value: str, expected: str
    ) -> None:
        """Debe canonicalizar etiquetas válidas manteniendo convención de texto."""
        assert canonicalize_label_or_unknown(value) == expected
