CREATE TABLE alert (
    id                  UUID            PRIMARY KEY,
    user_id             VARCHAR(255)    NOT NULL,
    product_url         VARCHAR(2048)   NOT NULL,
    product_name        VARCHAR(500),
    target_price        NUMERIC(12, 2)  NOT NULL,
    last_observed_price NUMERIC(12, 2),
    status              VARCHAR(20)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    last_checked_at     TIMESTAMPTZ
);

CREATE INDEX idx_alert_user_id ON alert (user_id);
CREATE INDEX idx_alert_active ON alert (status) WHERE status = 'ACTIVE';
