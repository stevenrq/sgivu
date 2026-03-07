"""Cliente HTTP para el microservicio de compra-venta.

Responsabilidad única: comunicación con sgivu-purchase-sale.
"""

from __future__ import annotations

import logging
from datetime import date
from typing import Any, Dict, List, Optional

import httpx

from app.infrastructure.config import Settings

logger = logging.getLogger(__name__)


class PurchaseSaleClient:
    """Cliente async para interactuar con el microservicio de compra-venta."""

    def __init__(self, settings: Settings) -> None:
        self._base_url = settings.sgivu_purchase_sale_url.rstrip("/")
        self._timeout = settings.request_timeout_seconds
        self._internal_key = settings.service_internal_secret_key

    def _headers(self) -> Dict[str, str]:
        headers: Dict[str, str] = {"Accept": "application/json"}
        if self._internal_key:
            headers["X-Internal-Service-Key"] = self._internal_key
        return headers

    async def fetch_contracts(
        self,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> List[Dict[str, Any]]:
        """Obtiene contratos paginados desde el servicio de compra-venta."""
        results: List[Dict[str, Any]] = []
        page = 0
        size = 200

        async with httpx.AsyncClient(timeout=self._timeout) as client:
            while True:
                params: Dict[str, Any] = {
                    "page": page,
                    "size": size,
                    "detailed": False,
                }
                if start_date:
                    params["startDate"] = start_date.isoformat()
                if end_date:
                    params["endDate"] = end_date.isoformat()

                response = await client.get(
                    f"{self._base_url}/v1/purchase-sales/search",
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

        logger.info("Retrieved %s purchase/sale contracts", len(results))
        return results
