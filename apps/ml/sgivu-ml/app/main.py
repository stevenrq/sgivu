"""Punto de entrada de la aplicación FastAPI sgivu-ml.

Configura el lifespan async, registra exception handlers y
monta los routers de la API.
"""

from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI

from app.api.exception_handlers import register_exception_handlers
from app.api.routers import health, prediction
from app.infrastructure.config import get_settings
from app.infrastructure.persistence.database import (
    close_engine,
    database_enabled,
    init_engine,
)
from app.infrastructure.persistence.migration import run_migrations


@asynccontextmanager
async def lifespan(app_: FastAPI) -> AsyncIterator[None]:
    """Gestiona startup y shutdown del engine async de base de datos."""
    settings = get_settings()
    if database_enabled(settings):
        if settings.database_run_migrations:
            run_migrations(settings)
        await init_engine(settings)
    yield
    await close_engine()


settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

register_exception_handlers(app)
app.include_router(health.router)
app.include_router(prediction.router)
