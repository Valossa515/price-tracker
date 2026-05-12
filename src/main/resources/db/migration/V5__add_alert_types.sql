ALTER TABLE alert
    ALTER COLUMN target_price DROP NOT NULL,
    ADD COLUMN alert_type              VARCHAR(30)    NOT NULL DEFAULT 'PRICE_BELOW_TARGET',
    ADD COLUMN discount_percent        NUMERIC(5, 2),
    ADD COLUMN drop_window_days        INT,
    ADD COLUMN drop_percent            NUMERIC(5, 2),
    ADD COLUMN last_observed_available BOOLEAN,
    ADD COLUMN real_discount_flag      BOOLEAN;

ALTER TABLE price_check_history
    ADD COLUMN available BOOLEAN;
