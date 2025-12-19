from __future__ import annotations

import asyncio
import logging
from datetime import date
from typing import Any, Dict, Iterable, List, Optional, Tuple

import httpx
import pandas as pd

from app.core.config import Settings, get_settings

logger = logging.getLogger(__name__)


class PurchaseSaleClient:
    """Cliente HTTP para recuperar contratos de compra/venta via gateway."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.base_url = settings.sgivu_purchase_sale_url.rstrip("/")

    def _headers(self) -> Dict[str, str]:
        """Encabezados internos; otros microservicios validan X-Internal-Service-Key."""
        headers = {"Accept": "application/json"}
        if self.settings.service_internal_secret_key:
            headers["X-Internal-Service-Key"] = (
                self.settings.service_internal_secret_key
            )
        return headers

    async def fetch_contracts(
        self, start_date: Optional[date] = None, end_date: Optional[date] = None
    ) -> List[Dict[str, Any]]:
        """Paginación de contratos con filtro temporal opcional.

        Args:
            start_date: Fecha mínima ISO (filtra updatedAt/createdAt en el MS).
            end_date: Fecha máxima ISO.

        Returns:
            Lista de dicts con contratos crudos (estructura de PurchaseSaleController).
        """
        results: List[Dict[str, Any]] = []
        page = 0
        size = 200

        async with httpx.AsyncClient(
            timeout=self.settings.request_timeout_seconds
        ) as client:
            while True:
                params: Dict[str, Any] = {"page": page, "size": size, "detailed": False}
                if start_date:
                    params["startDate"] = start_date.isoformat()
                if end_date:
                    params["endDate"] = end_date.isoformat()

                response = await client.get(
                    f"{self.base_url}/v1/purchase-sales/search",
                    params=params,
                    headers=self._headers(),
                )
                response.raise_for_status()
                payload = response.json()
                content = payload.get("content", payload)
                if not content:
                    break

                results.extend(content)
                total_pages = payload.get("totalPages")
                if total_pages is None or page >= total_pages - 1:
                    break
                page += 1

        logger.info("Se recuperaron %s contratos de compra/venta", len(results))
        return results


class VehicleClient:
    """Cliente HTTP para enriquecer contratos con detalle del inventario."""

    def __init__(self, settings: Settings, concurrency: int = 10) -> None:
        self.settings = settings
        self.base_url = settings.sgivu_vehicle_url.rstrip("/")
        self._semaphore = asyncio.Semaphore(concurrency)

    def _headers(self) -> Dict[str, str]:
        """Encabezados internos; envía la clave compartida para saltar auth externa."""
        headers = {"Accept": "application/json"}
        if self.settings.service_internal_secret_key:
            headers["X-Internal-Service-Key"] = (
                self.settings.service_internal_secret_key
            )
        return headers

    async def fetch_vehicle(
        self, vehicle_id: int, vehicle_type: str | None
    ) -> Dict[str, Any]:
        """Obtiene detalle de un vehículo desde el gateway.

        Selecciona endpoint según tipo (cars/motorcycles) y hace fallback si
        no se conoce el tipo con certeza.
        """
        endpoints = ["cars", "motorcycles"]
        if vehicle_type == "CAR":
            endpoints = ["cars"]
        elif vehicle_type == "MOTORCYCLE":
            endpoints = ["motorcycles"]

        async with self._semaphore:
            async with httpx.AsyncClient(
                timeout=self.settings.request_timeout_seconds
            ) as client:
                for endpoint in endpoints:
                    url = f"{self.base_url}/v1/{endpoint}/{vehicle_id}"
                    response = await client.get(url, headers=self._headers())
                    if response.status_code == 404:
                        continue
                    response.raise_for_status()
                    payload = response.json()
                    resolved_type = (
                        vehicle_type
                        or payload.get("vehicleType")
                        or payload.get("type")
                        or ("CAR" if endpoint == "cars" else "MOTORCYCLE")
                    )
                    payload["vehicleType"] = resolved_type
                    return payload

        logger.warning("Vehiculo %s no encontrado en inventario", vehicle_id)
        return {}

    async def fetch_bulk(
        self, vehicles: Iterable[Tuple[int, Optional[str]]]
    ) -> Dict[int, Dict[str, Any]]:
        """Recupera en paralelo múltiples vehículos para minimizar latencia."""
        vehicles = [(vid, vtype) for vid, vtype in vehicles if vid]
        tasks = [
            self.fetch_vehicle(vehicle_id, vehicle_type)
            for vehicle_id, vehicle_type in vehicles
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        mapped: Dict[int, Dict[str, Any]] = {}
        for (vehicle_id, _), result in zip(vehicles, results):
            if isinstance(result, Exception) or not isinstance(result, dict):
                logger.error("Error recuperando vehiculo %s: %s", vehicle_id, result)
                continue
            mapped[vehicle_id] = result
        return mapped


class DemandDatasetLoader:
    """Orquesta la recuperación de datos crudos (compras/ventas + inventario).

    - Descarga contratos desde purchase-sale.
    - Enriquecer con atributos de inventario (marca/modelo/línea/año/kilometraje).
    - Devuelve un DataFrame homogéneo para ingeniería de características.
    """

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.purchase_client = PurchaseSaleClient(self.settings)
        self.vehicle_client = VehicleClient(self.settings)

    async def load_transactions(
        self, start_date: Optional[date] = None, end_date: Optional[date] = None
    ) -> pd.DataFrame:
        """Descarga y fusiona contratos con detalle de vehículo.

        Args:
            start_date: Fecha mínima (opcional) para reducir el historial.
            end_date: Fecha máxima (opcional).

        Returns:
            DataFrame con columnas normalizadas para el pipeline de entrenamiento.
        """
        contracts = await self.purchase_client.fetch_contracts(
            start_date=start_date, end_date=end_date
        )
        if not contracts:
            return pd.DataFrame()

        vehicles_hint: list[tuple[int, Optional[str]]] = []
        for contract in contracts:
            vid = contract.get("vehicleId")
            vtype = (contract.get("vehicleSummary") or {}).get("type")
            if vid is not None:
                vehicles_hint.append((int(vid), vtype))
        vehicle_map = await self.vehicle_client.fetch_bulk(vehicles_hint)

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
