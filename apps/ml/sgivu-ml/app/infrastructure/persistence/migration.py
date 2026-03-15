"""Ejecutor de migraciones Alembic en el startup de la aplicación.

Equivalente al auto-run de Flyway en Spring Boot: ejecuta
``alembic upgrade head`` programáticamente antes de aceptar requests.
"""

from __future__ import annotations

import logging
from pathlib import Path

from alembic import command
from alembic.config import Config

from app.infrastructure.config import Settings

logger = logging.getLogger(__name__)

_PROJECT_ROOT = Path(__file__).resolve().parents[3]


def run_migrations(settings: Settings) -> None:
    """Aplica todas las migraciones pendientes (``upgrade head``)."""
    dsn = settings.database_sync_dsn()
    if not dsn:
        logger.warning("Database DSN not configured — skipping migrations")
        return

    alembic_cfg = Config(str(_PROJECT_ROOT / "alembic.ini"))
    alembic_cfg.set_main_option("script_location", str(_PROJECT_ROOT / "alembic"))
    alembic_cfg.set_main_option("sqlalchemy.url", dsn)

    logger.info("Running Alembic migrations (upgrade head)…")
    command.upgrade(alembic_cfg, "head")
    logger.info("Alembic migrations completed")
