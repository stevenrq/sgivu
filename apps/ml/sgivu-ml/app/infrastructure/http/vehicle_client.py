"""Cliente HTTP para el microservicio de inventario de vehículos.

Responsabilidad única: comunicación con sgivu-vehicle.
"""

from __future__ import annotations

import asyncio
import logging
from typing import Any, Dict, Iterable, Optional, Tuple

import httpx

from app.infrastructure.config import Settings

logger = logging.getLogger(__name__)


class VehicleClient:
    """Cliente async para interactuar con el microservicio de vehículos."""

    def __init__(self, settings: Settings, concurrency: int = 10) -> None:
        self._base_url = settings.sgivu_vehicle_url.rstrip("/")
        self._timeout = settings.request_timeout_seconds
        self._internal_key = settings.service_internal_secret_key
        self._semaphore = asyncio.Semaphore(concurrency)

    def _headers(self) -> Dict[str, str]:
        """Encabezados internos; envía la clave compartida para saltar auth externa."""
        headers: Dict[str, str] = {"Accept": "application/json"}
        if self._internal_key:
            headers["X-Internal-Service-Key"] = self._internal_key
        return headers

    async def fetch_vehicle(
        self, vehicle_id: int, vehicle_type: str | None
    ) -> Dict[str, Any]:
        """Obtiene los detalles de un vehículo por su ID."""
        endpoints = ["cars", "motorcycles"]
        if vehicle_type == "CAR":
            endpoints = ["cars"]
        elif vehicle_type == "MOTORCYCLE":
            endpoints = ["motorcycles"]

        async with self._semaphore:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                for endpoint in endpoints:
                    url = f"{self._base_url}/v1/{endpoint}/{vehicle_id}"
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

        logger.warning("Vehicle %s not found in inventory", vehicle_id)
        return {}

    async def fetch_bulk(
        self, vehicles: Iterable[Tuple[int, Optional[str]]]
    ) -> Dict[int, Dict[str, Any]]:
        """Obtiene detalles de múltiples vehículos en paralelo."""
        vehicle_list = [(vid, vtype) for vid, vtype in vehicles if vid]
        tasks = [
            self.fetch_vehicle(vehicle_id, vehicle_type)
            for vehicle_id, vehicle_type in vehicle_list
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        mapped: Dict[int, Dict[str, Any]] = {}
        for (vehicle_id, _), result in zip(vehicle_list, results):
            if isinstance(result, Exception) or not isinstance(result, dict):
                logger.error("Error fetching vehicle %s: %s", vehicle_id, result)
                continue
            mapped[vehicle_id] = result
        return mapped
