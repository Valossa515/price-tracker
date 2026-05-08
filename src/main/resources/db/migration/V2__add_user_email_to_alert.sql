ALTER TABLE alert ADD COLUMN user_email VARCHAR(320);
UPDATE alert SET user_email = 'unknown@unknown.local' WHERE user_email IS NULL;
ALTER TABLE alert ALTER COLUMN user_email SET NOT NULL;
