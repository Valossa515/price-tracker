CREATE TABLE price_check_history (
    id              BIGSERIAL       PRIMARY KEY,
    alert_id        UUID            NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    observed_price  NUMERIC(12, 2)  NOT NULL,
    observed_at     TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_price_history_alert_time
    ON price_check_history (alert_id, observed_at DESC);
