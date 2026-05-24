"""Tests para HttpTransactionLoader."""

from __future__ import annotations

from unittest.mock import AsyncMock

import pytest

from app.infrastructure.http.transaction_loader import HttpTransactionLoader


class TestHttpTransactionLoader:
    """HttpTransactionLoader"""

    @pytest.mark.asyncio
    async def test_normalizes_missing_aliases_to_unknown(self) -> None:
        """Debe convertir aliases de faltantes a UNKNOWN en salida del loader."""
        purchase_client = AsyncMock()
        vehicle_client = AsyncMock()

        purchase_client.fetch_contracts.return_value = [
            {
                "id": 1,
                "contractType": "SALE",
                "contractStatus": "ACTIVE",
                "clientId": 10,
                "userId": 20,
                "vehicleId": 99,
                "purchasePrice": 100.0,
                "salePrice": 150.0,
                "paymentMethod": "CASH",
                "observations": "",
                "createdAt": "2026-01-10T10:00:00",
                "updatedAt": "2026-01-12T11:00:00",
                "vehicleSummary": {
                    "type": "N/D",
                    "brand": "N/D",
                    "model": "N/D",
                    "line": "N/D",
                    "status": "N/D",
                },
            }
        ]
        vehicle_client.fetch_bulk.return_value = {
            99: {
                "vehicleType": "ND",
                "brand": "ND",
                "model": "ND",
                "line": "ND",
                "status": "ND",
            }
        }

        loader = HttpTransactionLoader(purchase_client, vehicle_client)
        result = await loader.load_transactions()

        assert len(result) == 1
        row = result.iloc[0]
        assert row["vehicle_type"] == "UNKNOWN"
        assert row["brand"] == "UNKNOWN"
        assert row["model"] == "UNKNOWN"
        assert row["line"] == "UNKNOWN"
        assert row["vehicle_status"] == "UNKNOWN"

    @pytest.mark.asyncio
    async def test_uses_summary_line_when_vehicle_details_miss_line(self) -> None:
        """Debe usar line del summary cuando el detalle no trae line."""
        purchase_client = AsyncMock()
        vehicle_client = AsyncMock()

        purchase_client.fetch_contracts.return_value = [
            {
                "id": 2,
                "contractType": "SALE",
                "contractStatus": "ACTIVE",
                "clientId": 10,
                "userId": 20,
                "vehicleId": 77,
                "purchasePrice": 100.0,
                "salePrice": 150.0,
                "paymentMethod": "CASH",
                "observations": "",
                "createdAt": "2026-02-10T10:00:00",
                "updatedAt": "2026-02-12T11:00:00",
                "vehicleSummary": {
                    "type": "CAR",
                    "brand": "Toyota",
                    "model": "Corolla",
                    "line": "XEi",
                    "status": "AVAILABLE",
                },
            }
        ]
        vehicle_client.fetch_bulk.return_value = {
            77: {
                "vehicleType": "CAR",
                "brand": "Toyota",
                "model": "Corolla",
            }
        }

        loader = HttpTransactionLoader(purchase_client, vehicle_client)
        result = await loader.load_transactions()

        assert len(result) == 1
        row = result.iloc[0]
        assert row["line"] == "XEi"
        assert row["vehicle_status"] == "AVAILABLE"

    @pytest.mark.asyncio
    async def test_returns_empty_dataframe_when_contracts_are_missing(self) -> None:
        """Debe retornar DataFrame vacío cuando no hay contratos."""
        purchase_client = AsyncMock()
        vehicle_client = AsyncMock()

        purchase_client.fetch_contracts.return_value = []

        loader = HttpTransactionLoader(purchase_client, vehicle_client)
        result = await loader.load_transactions()

        assert result.empty
