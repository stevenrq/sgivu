"""Módulo de conexión asíncrona a la base de datos.

Gestiona el ciclo de vida del ``AsyncEngine`` y la fábrica de sesiones.
Se inicializa durante el *lifespan* de la aplicación FastAPI.
"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

from app.infrastructure.config import Settings


class Base(DeclarativeBase):
    """Base declarativa para los modelos ORM."""


_engine: AsyncEngine | None = None
_session_factory: async_sessionmaker[AsyncSession] | None = None


def database_enabled(settings: Settings) -> bool:
    """Indica si la base de datos está configurada."""
    return bool(settings.database_dsn())


async def init_engine(settings: Settings) -> AsyncEngine | None:
    """Crea el engine async y la fábrica de sesiones."""
    dsn = settings.database_dsn()
    if not dsn:
        return None

    global _engine, _session_factory
    _engine = create_async_engine(
        dsn,
        echo=settings.database_echo,
        pool_pre_ping=True,
    )
    _session_factory = async_sessionmaker(
        bind=_engine,
        class_=AsyncSession,
        autoflush=False,
        expire_on_commit=False,
    )
    return _engine


async def close_engine() -> None:
    """Cierra el engine y libera las conexiones del pool."""
    global _engine, _session_factory
    if _engine:
        await _engine.dispose()
    _engine = None
    _session_factory = None


def get_session_factory() -> async_sessionmaker[AsyncSession] | None:
    """Devuelve la fábrica de sesiones inicializada o ``None``."""
    return _session_factory
