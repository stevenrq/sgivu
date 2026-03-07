"""Implementación HTTP del cargador de transacciones.

Implementa ``TransactionLoaderPort`` orquestando los clientes de
compra-venta y vehículos para construir un DataFrame de transacciones.
"""

from __future__ import annotations

import logging
from datetime import date
from typing import Any, Dict, List, Optional

import pandas as pd

from app.infrastructure.http.purchase_sale_client import PurchaseSaleClient
from app.infrastructure.http.vehicle_client import VehicleClient

logger = logging.getLogger(__name__)


class HttpTransactionLoader:
    """Carga transacciones combinando datos de compra-venta e inventario."""

    def __init__(
        self,
        purchase_client: PurchaseSaleClient,
        vehicle_client: VehicleClient,
    ) -> None:
        self._purchase_client = purchase_client
        self._vehicle_client = vehicle_client

    async def load_transactions(
        self,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> pd.DataFrame:
        """Carga contratos y enriquece con datos de vehículos."""
        contracts = await self._purchase_client.fetch_contracts(
            start_date=start_date, end_date=end_date
        )
        if not contracts:
            return pd.DataFrame()

        # Recopila hints de vehículos para la carga masiva.
        vehicles_hint: list[tuple[int, Optional[str]]] = []
        for contract in contracts:
            vid = contract.get("vehicleId")
            vtype = (contract.get("vehicleSummary") or {}).get("type")
            if vid is not None:
                vehicles_hint.append((int(vid), vtype))
        vehicle_map = await self._vehicle_client.fetch_bulk(vehicles_hint)

        rows: List[Dict[str, Any]] = []
        for contract in contracts:
            vehicle_summary = contract.get("vehicleSummary") or {}
            vehicle_id = contract.get("vehicleId")
            vehicle_details = (
                vehicle_map.get(int(vehicle_id)) or {} if vehicle_id is not None else {}
            )

            rows.append(
                {
                    "contract_id": contract.get("id"),
                    "contract_type": contract.get("contractType"),
                    "contract_status": contract.get("contractStatus"),
                    "client_id": contract.get("clientId"),
                    "user_id": contract.get("userId"),
                    "vehicle_id": contract.get("vehicleId"),
                    "purchase_price": contract.get("purchasePrice"),
                    "sale_price": contract.get("salePrice"),
                    "payment_method": contract.get("paymentMethod"),
                    "observations": contract.get("observations"),
                    "created_at": contract.get("createdAt"),
                    "updated_at": contract.get("updatedAt"),
                    "vehicle_type": vehicle_summary.get("type")
                    or vehicle_details.get("vehicleType")
                    or vehicle_details.get("type"),
                    "brand": vehicle_details.get("brand")
                    or vehicle_summary.get("brand"),
                    "model": vehicle_details.get("model")
                    or vehicle_summary.get("model"),
                    "line": vehicle_details.get("line"),
                    "year": vehicle_details.get("year"),
                    "mileage": vehicle_details.get("mileage"),
                    "vehicle_status": vehicle_summary.get("status")
                    or vehicle_details.get("status")
                    or vehicle_details.get("vehicleStatus"),
                }
            )

        return pd.DataFrame(rows)
