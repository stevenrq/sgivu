from __future__ import annotations

from contextlib import contextmanager
from typing import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.core.config import Settings, get_settings


class Base(DeclarativeBase):
    """Base declarativa para los modelos SQLAlchemy."""


_ENGINE = None
_SESSION_FACTORY: sessionmaker[Session] | None = None


def database_enabled(settings: Settings | None = None) -> bool:
    """Indica si la base de datos está configurada."""
    active_settings = settings or get_settings()
    return bool(active_settings.database_dsn())


def get_engine(settings: Settings | None = None):
    """Devuelve el engine singleton de SQLAlchemy si hay URL configurada."""
    active_settings = settings or get_settings()
    database_url = active_settings.database_dsn()
    if not database_url:
        return None

    global _ENGINE
    if _ENGINE is None:
        _ENGINE = create_engine(
            database_url,
            echo=active_settings.database_echo,
            pool_pre_ping=True,
            future=True,
        )
    return _ENGINE


def get_session_factory(
    settings: Settings | None = None,
) -> sessionmaker[Session] | None:
    """Crea un sessionmaker asociado al engine configurado."""
    engine = get_engine(settings)
    if engine is None:
        return None

    global _SESSION_FACTORY
    if _SESSION_FACTORY is None:
        _SESSION_FACTORY = sessionmaker(
            bind=engine,
            autoflush=False,
            autocommit=False,
            expire_on_commit=False,
        )
    return _SESSION_FACTORY


@contextmanager
def session_scope(settings: Settings | None = None) -> Iterator[Session]:
    """Scope transaccional para operaciones con la base de datos."""
    session_factory = get_session_factory(settings)
    if session_factory is None:
        raise RuntimeError("DATABASE_URL no configurada; no hay sesión disponible.")
    session = session_factory()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def init_db(settings: Settings | None = None) -> None:
    """Crea tablas si la base de datos está activa."""
    engine = get_engine(settings)
    if engine is None:
        return
    from app.database import models  # noqa: F401 - asegura registro de metadata

    Base.metadata.create_all(bind=engine)
