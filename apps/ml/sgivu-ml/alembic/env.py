"""Entorno de ejecución de migraciones Alembic.

Configura la conexión a la base de datos usando los mismos ajustes
que la aplicación FastAPI (``app.infrastructure.config.Settings``).
Soporta ejecución online (contra BD real) y offline (generación de SQL).
"""

from logging.config import fileConfig

from alembic import context
from sqlalchemy import create_engine, pool

from app.infrastructure.config import get_settings
from app.infrastructure.persistence.database import Base
from app.infrastructure.persistence import models  # noqa: F401

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata


def _get_sync_url() -> str:
    """Obtiene el DSN sync (psycopg) desde la configuración de la aplicación."""
    settings = get_settings()
    dsn = settings.database_dsn()
    if not dsn:
        raise RuntimeError(
            "Database DSN is not configured. "
            "Set DATABASE_URL or the DB_* environment variables."
        )
    return dsn.replace("+asyncpg", "+psycopg")


def run_migrations_offline() -> None:
    """Ejecuta migraciones en modo 'offline' (genera SQL sin conectar a la BD)."""
    context.configure(
        url=_get_sync_url(),
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Ejecuta migraciones en modo 'online' (conecta a la BD)."""
    connectable = create_engine(
        _get_sync_url(),
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
        )
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
