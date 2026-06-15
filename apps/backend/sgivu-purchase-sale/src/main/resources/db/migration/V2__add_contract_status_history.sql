CREATE SEQUENCE IF NOT EXISTS contract_status_history_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS contract_status_history
(
    id               BIGINT PRIMARY KEY                DEFAULT nextval('contract_status_history_id_seq'),
    purchase_sale_id BIGINT                   NOT NULL,
    previous_status  VARCHAR(50)              NULL,
    new_status       VARCHAR(50)              NOT NULL,
    changed_by       BIGINT                   NULL,
    changed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason           VARCHAR(300)             NULL,
    CONSTRAINT fk_status_history_purchase_sale FOREIGN KEY (purchase_sale_id) REFERENCES purchase_sales (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_status_history_purchase_sale_id ON contract_status_history (purchase_sale_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON contract_status_history (changed_at);
