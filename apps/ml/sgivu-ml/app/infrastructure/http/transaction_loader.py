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
from app.infrastructure.ml.normalization import normalize_unknown_alias

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
        """Carga contratos y enriquece con datos de vehículos.

        Solicita contratos al servicio de compra/venta y realiza una consulta
        masiva al servicio de vehículos para enriquecer los registros. Devuelve
        un DataFrame apto para pasar a `FeatureEngineering.build_feature_table`.

        Parameters
        ----------
        start_date : date | None
            Fecha de inicio para filtrar contratos (inclusive).
        end_date : date | None
            Fecha de fin para filtrar contratos (inclusive).

        Returns
        -------
        pd.DataFrame
            DataFrame con filas por contrato enriquecidas con información de
            vehículo (`brand`, `model`, `line`, `vehicle_type`, etc.). Si no
            hay contratos devuelve un DataFrame vacío.
        """
        contracts = await self._purchase_client.fetch_contracts(
            start_date=start_date, end_date=end_date
        )
        if not contracts:
            return pd.DataFrame()

        # Recopila hints de vehículos para la carga masiva, sin duplicados.
        seen_vehicles: set[int] = set()
        vehicles_hint: list[tuple[int, Optional[str]]] = []
        for contract in contracts:
            vid = contract.get("vehicleId")
            if vid is None:
                continue
            vid_int = int(vid)
            if vid_int not in seen_vehicles:
                seen_vehicles.add(vid_int)
                vtype = (contract.get("vehicleSummary") or {}).get("type")
                vehicles_hint.append((vid_int, vtype))
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
                    "vehicle_type": normalize_unknown_alias(
                        vehicle_summary.get("type")
                        or vehicle_details.get("vehicleType")
                        or vehicle_details.get("type")
                    ),
                    "brand": normalize_unknown_alias(
                        vehicle_details.get("brand") or vehicle_summary.get("brand")
                    ),
                    "model": normalize_unknown_alias(
                        vehicle_details.get("model") or vehicle_summary.get("model")
                    ),
                    "line": normalize_unknown_alias(
                        vehicle_details.get("line") or vehicle_summary.get("line")
                    ),
                    "year": vehicle_details.get("year"),
                    "mileage": vehicle_details.get("mileage"),
                    "vehicle_status": normalize_unknown_alias(
                        vehicle_summary.get("status")
                        or vehicle_details.get("status")
                        or vehicle_details.get("vehicleStatus")
                    ),
                }
            )

        return pd.DataFrame(rows)
