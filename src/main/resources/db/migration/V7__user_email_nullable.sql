-- Webhook-only alerts (created via /api/v1/public/alerts) don't require an email.
ALTER TABLE alert
    ALTER COLUMN user_email DROP NOT NULL;
