-- V6: P2~P3 지원 컬럼 보강

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS user_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS notification_enabled_yn CHAR(1) DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS mfa_enabled_yn CHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS mfa_type_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS last_password_changed_at TIMESTAMP;

ALTER TABLE corporates
    ADD COLUMN IF NOT EXISTS established_date DATE,
    ADD COLUMN IF NOT EXISTS corporate_type_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS website VARCHAR(500);

ALTER TABLE corporates
    ALTER COLUMN representative_name DROP NOT NULL;

ALTER TABLE kyc_applications
    ADD COLUMN IF NOT EXISTS application_channel_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS finance_institution_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS finance_branch_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS finance_staff_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS finance_customer_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS visited_at TIMESTAMP;

ALTER TABLE kyc_documents
    ADD COLUMN IF NOT EXISTS uploaded_by_type_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS uploaded_by_user_id BIGINT;

ALTER TABLE credentials
    ADD COLUMN IF NOT EXISTS offer_token_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS offer_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS offer_used_yn CHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS holder_did VARCHAR(255),
    ADD COLUMN IF NOT EXISTS holder_xrpl_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS wallet_saved_at TIMESTAMP;

ALTER TABLE vp_verifications
    ADD COLUMN IF NOT EXISTS verifier_id BIGINT,
    ADD COLUMN IF NOT EXISTS finance_institution_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS request_type_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS test_yn CHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS re_auth_yn CHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS permission_result_json TEXT,
    ADD COLUMN IF NOT EXISTS callback_status_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS callback_sent_at TIMESTAMP;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS channel_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS target_type_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS target_id BIGINT,
    ADD COLUMN IF NOT EXISTS template_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sent_status_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS before_value_json TEXT,
    ADD COLUMN IF NOT EXISTS after_value_json TEXT;
