"""Puerto para cargar transacciones desde fuentes externas.

Abstrae la obtención de datos de compra/venta independientemente
de si vienen de microservicios HTTP, archivos CSV u otra fuente.
"""

from __future__ import annotations

from datetime import date
from typing import Optional, Protocol, runtime_checkable

import pandas as pd


@runtime_checkable
class TransactionLoaderPort(Protocol):
    """Contrato para cargar datos de transacciones de compra/venta."""

    async def load_transactions(
        self,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> pd.DataFrame:
        """Carga transacciones, opcionalmente filtradas por rango de fechas."""
        ...
