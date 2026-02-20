CREATE SEQUENCE IF NOT EXISTS purchase_sales_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS purchase_sales
(
    id                  BIGINT PRIMARY KEY                DEFAULT nextval('purchase_sales_id_seq'),
    client_id           BIGINT                   NULL,
    user_id             BIGINT                   NULL,
    vehicle_id          BIGINT                   NULL,
    purchase_price      DOUBLE PRECISION         NOT NULL CHECK (purchase_price >= 0),
    sale_price          DOUBLE PRECISION         NOT NULL CHECK (sale_price >= 0),
    contract_type       VARCHAR(50)              NOT NULL,
    contract_status     VARCHAR(50)              NOT NULL,
    payment_limitations VARCHAR(200)             NOT NULL,
    payment_terms       VARCHAR(200)             NOT NULL,
    payment_method      VARCHAR(50)              NOT NULL,
    observations        VARCHAR(500)             NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_purchase_sales_client_id ON purchase_sales (client_id);
CREATE INDEX IF NOT EXISTS idx_purchase_sales_user_id ON purchase_sales (user_id);
CREATE INDEX IF NOT EXISTS idx_purchase_sales_vehicle_id ON purchase_sales (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_purchase_sales_contract_status ON purchase_sales (contract_status);
CREATE INDEX IF NOT EXISTS idx_purchase_sales_contract_type ON purchase_sales (contract_type);
CREATE INDEX IF NOT EXISTS idx_purchase_sales_created_at ON purchase_sales (created_at);
