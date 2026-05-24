"""Esquema inicial — tablas de artefactos, features y predicciones

Revision ID: 0001
Revises: None
Create Date: 2026-03-11
"""

from typing import Sequence, Union

from alembic import op

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        """
        CREATE TABLE IF NOT EXISTS ml_model_artifacts (
            id BIGSERIAL PRIMARY KEY,
            model_name VARCHAR(128) NOT NULL,
            version VARCHAR(32) NOT NULL UNIQUE,
            model_metadata JSONB NOT NULL,
            artifact BYTEA NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_model_artifacts_model_name
            ON ml_model_artifacts (model_name)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_model_artifacts_version
            ON ml_model_artifacts (version)
    """
    )

    op.execute(
        """
        CREATE TABLE IF NOT EXISTS ml_training_features (
            id BIGSERIAL PRIMARY KEY,
            model_version VARCHAR(32) NOT NULL,
            event_month DATE NOT NULL,
            vehicle_type VARCHAR(32) NOT NULL,
            brand VARCHAR(120) NOT NULL,
            model VARCHAR(120) NOT NULL,
            line VARCHAR(120) NOT NULL,
            sales_count DOUBLE PRECISION NOT NULL,
            purchases_count DOUBLE PRECISION NOT NULL,
            avg_margin DOUBLE PRECISION NOT NULL,
            avg_sale_price DOUBLE PRECISION NOT NULL,
            avg_purchase_price DOUBLE PRECISION NOT NULL,
            avg_days_inventory DOUBLE PRECISION NOT NULL,
            inventory_rotation DOUBLE PRECISION NOT NULL,
            lag_1 DOUBLE PRECISION NOT NULL,
            lag_3 DOUBLE PRECISION NOT NULL,
            lag_6 DOUBLE PRECISION NOT NULL,
            rolling_mean_3 DOUBLE PRECISION NOT NULL,
            rolling_mean_6 DOUBLE PRECISION NOT NULL,
            month INTEGER NOT NULL,
            year INTEGER NOT NULL,
            month_sin DOUBLE PRECISION NOT NULL,
            month_cos DOUBLE PRECISION NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            CONSTRAINT uq_ml_training_feature UNIQUE (
                model_version, vehicle_type, brand, model, line, event_month
            )
        )
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_model_version
            ON ml_training_features (model_version)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_event_month
            ON ml_training_features (event_month)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_vehicle_type
            ON ml_training_features (vehicle_type)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_brand
            ON ml_training_features (brand)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_model
            ON ml_training_features (model)
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_training_features_line
            ON ml_training_features (line)
    """
    )

    op.execute(
        """
        CREATE TABLE IF NOT EXISTS ml_predictions (
            id BIGSERIAL PRIMARY KEY,
            model_version VARCHAR(32) NOT NULL,
            request_payload JSONB NOT NULL,
            response_payload JSONB NOT NULL,
            vehicle_type VARCHAR(32),
            brand VARCHAR(120),
            model VARCHAR(120),
            line VARCHAR(120),
            horizon_months INTEGER,
            confidence DOUBLE PRECISION,
            with_history BOOLEAN NOT NULL DEFAULT FALSE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
    """
    )
    op.execute(
        """
        CREATE INDEX IF NOT EXISTS ix_ml_predictions_model_version
            ON ml_predictions (model_version)
    """
    )


def downgrade() -> None:
    op.execute("DROP TABLE IF EXISTS ml_predictions")
    op.execute("DROP TABLE IF EXISTS ml_training_features")
    op.execute("DROP TABLE IF EXISTS ml_model_artifacts")
