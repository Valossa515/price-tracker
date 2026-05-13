ALTER TABLE alert
    ADD COLUMN webhook_url    VARCHAR(2048),
    ADD COLUMN webhook_secret VARCHAR(128);
