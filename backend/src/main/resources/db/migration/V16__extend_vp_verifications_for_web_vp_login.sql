-- V16: 웹 VP 로그인용 vp_verifications 확장

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'vp_verifications'
          AND column_name = 'credential_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE vp_verifications
            ALTER COLUMN credential_id DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'vp_verifications'
          AND column_name = 'corporate_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE vp_verifications
            ALTER COLUMN corporate_id DROP NOT NULL;
    END IF;
END $$;

ALTER TABLE vp_verifications
    ADD COLUMN IF NOT EXISTS qr_token_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS browser_session_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS login_completed_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vp_verifications_qr_token_hash
    ON vp_verifications (qr_token_hash)
    WHERE qr_token_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vp_verifications_status_expires
    ON vp_verifications (vp_verification_status_code, expires_at);
