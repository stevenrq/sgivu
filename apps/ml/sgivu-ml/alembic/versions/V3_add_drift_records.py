"""Agregar tabla de registros de drift para monitoreo

Revision ID: 0003
Revises: 0002
Create Date: 2026-03-28
"""

from typing import Sequence, Union

from alembic import op

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        """
        CREATE TABLE IF NOT EXISTS ml_drift_records (
            id BIGSERIAL PRIMARY KEY,
            model_version VARCHAR(32) NOT NULL,
            vehicle_type VARCHAR(32),
            brand VARCHAR(120),
            model VARCHAR(120),
            line VARCHAR(120),
            predicted_month VARCHAR(10) NOT NULL,
            predicted_demand DOUBLE PRECISION,
            actual_demand DOUBLE PRECISION NOT NULL,
            absolute_error DOUBLE PRECISION,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );

        CREATE INDEX IF NOT EXISTS idx_ml_drift_model_version
            ON ml_drift_records (model_version);
        """
    )


def downgrade() -> None:
    op.execute("DROP TABLE IF EXISTS ml_drift_records;")
