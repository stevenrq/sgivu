"""Agregar columnas de features estacionales trimestrales

Revision ID: 0002
Revises: 0001
Create Date: 2026-03-28
"""

from typing import Sequence, Union

from alembic import op

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        """
        ALTER TABLE ml_training_features
            ADD COLUMN IF NOT EXISTS quarter INTEGER DEFAULT 0,
            ADD COLUMN IF NOT EXISTS quarter_sin DOUBLE PRECISION DEFAULT 0,
            ADD COLUMN IF NOT EXISTS quarter_cos DOUBLE PRECISION DEFAULT 0;
        """
    )


def downgrade() -> None:
    op.execute(
        """
        ALTER TABLE ml_training_features
            DROP COLUMN IF EXISTS quarter,
            DROP COLUMN IF EXISTS quarter_sin,
            DROP COLUMN IF EXISTS quarter_cos;
        """
    )
