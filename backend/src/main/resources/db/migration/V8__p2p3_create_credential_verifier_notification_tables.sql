-- V8: Credential 요청/이력, Verifier, Callback, 알림 템플릿 테이블

CREATE TABLE IF NOT EXISTS credential_requests (
    credential_request_id BIGSERIAL PRIMARY KEY,
    credential_id BIGINT NOT NULL,
    request_type_code VARCHAR(30) NOT NULL,
    request_status_code VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    requested_by_type_code VARCHAR(30) NOT NULL,
    requested_by_id BIGINT,
    reason_code VARCHAR(50),
    reason TEXT,
    core_request_id VARCHAR(255),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_credential_requests_credential FOREIGN KEY (credential_id) REFERENCES credentials(credential_id),
    CONSTRAINT fk_credential_requests_core_request FOREIGN KEY (core_request_id) REFERENCES core_requests(core_request_id)
);

CREATE TABLE IF NOT EXISTS credential_status_histories (
    history_id BIGSERIAL PRIMARY KEY,
    credential_id BIGINT NOT NULL,
    before_status_code VARCHAR(30),
    after_status_code VARCHAR(30) NOT NULL,
    changed_by_type_code VARCHAR(30) NOT NULL,
    changed_by_id BIGINT,
    reason_code VARCHAR(50),
    reason TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credential_status_histories_credential FOREIGN KEY (credential_id) REFERENCES credentials(credential_id)
);

CREATE TABLE IF NOT EXISTS verifiers (
    verifier_id BIGSERIAL PRIMARY KEY,
    verifier_name VARCHAR(150) NOT NULL,
    verifier_status_code VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    contact_email VARCHAR(255),
    approved_at TIMESTAMP,
    suspended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS verifier_api_keys (
    api_key_id BIGSERIAL PRIMARY KEY,
    verifier_id BIGINT NOT NULL,
    key_name VARCHAR(100) NOT NULL,
    api_key_prefix VARCHAR(50) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    key_status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT fk_verifier_api_keys_verifier FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id)
);

CREATE TABLE IF NOT EXISTS verifier_callbacks (
    callback_id BIGSERIAL PRIMARY KEY,
    verifier_id BIGINT NOT NULL,
    callback_url VARCHAR(500) NOT NULL,
    callback_status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_verifier_callbacks_verifier FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id)
);

CREATE TABLE IF NOT EXISTS verifier_logs (
    verifier_log_id BIGSERIAL PRIMARY KEY,
    verifier_id BIGINT NOT NULL,
    api_key_id BIGINT,
    action_type_code VARCHAR(50) NOT NULL,
    request_path VARCHAR(300),
    method VARCHAR(10),
    status_code INT,
    result_code VARCHAR(50),
    latency_ms INT,
    client_sdk_version VARCHAR(50),
    policy_version VARCHAR(50),
    error_message TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verifier_logs_verifier FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id),
    CONSTRAINT fk_verifier_logs_api_key FOREIGN KEY (api_key_id) REFERENCES verifier_api_keys(api_key_id)
);

CREATE TABLE IF NOT EXISTS notification_templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    template_name VARCHAR(150) NOT NULL,
    channel_code VARCHAR(30) NOT NULL,
    title_template VARCHAR(255),
    message_template TEXT NOT NULL,
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vp_verifications_verifier'
          AND conrelid = 'vp_verifications'::regclass
    ) THEN
        ALTER TABLE vp_verifications
            ADD CONSTRAINT fk_vp_verifications_verifier
            FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id);
    END IF;
END $$;
