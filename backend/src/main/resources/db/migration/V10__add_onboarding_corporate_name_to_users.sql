ALTER TABLE users
    ADD COLUMN IF NOT EXISTS onboarding_corporate_name VARCHAR(255);
