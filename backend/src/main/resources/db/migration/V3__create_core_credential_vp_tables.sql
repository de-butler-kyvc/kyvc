-- V3: Core 연동, VC Credential, VP 검증, Verifier 테이블 생성 마이그레이션
-- 정의 테이블: core_requests, credentials, credential_offers, credential_requests, credential_status_histories, vp_verifications, verifiers, verifier_api_keys, verifier_callbacks, verifier_logs, did_institutions

-- 코어 내부 요청 추적 테이블
CREATE TABLE core_requests (
    core_request_id VARCHAR(255) NOT NULL,
    core_request_type_code VARCHAR(50) NOT NULL,
    core_target_type_code VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    core_request_status_code VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    request_payload_json TEXT,
    response_payload_json TEXT,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_core_requests PRIMARY KEY (core_request_id)
);

CREATE INDEX IDX_core_requests_target
ON core_requests (core_target_type_code, target_id);

CREATE INDEX IDX_core_requests_status
ON core_requests (core_request_status_code);

CREATE INDEX IDX_core_requests_requested_at
ON core_requests (requested_at);

-- Verifier 테이블
CREATE TABLE verifiers (
    verifier_id BIGSERIAL PRIMARY KEY,
    verifier_name VARCHAR(150) NOT NULL,
    verifier_status_code VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    contact_email VARCHAR(255),
    approved_at TIMESTAMP,
    suspended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 검증가능자격증명 메타데이터 테이블
CREATE TABLE credentials (
    credential_id BIGSERIAL,
    corporate_id BIGINT NOT NULL,
    kyc_id BIGINT NOT NULL,
    credential_external_id VARCHAR(255) NOT NULL,
    credential_type_code VARCHAR(100) NOT NULL,
    issuer_did VARCHAR(255) NOT NULL,
    credential_status_code VARCHAR(50) NOT NULL,
    vc_hash VARCHAR(255),
    xrpl_tx_hash VARCHAR(255),
    qr_token VARCHAR(255),
    qr_expires_at TIMESTAMP,
    issued_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wallet_saved_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    wallet_saved_at TIMESTAMP,
    wallet_device_id VARCHAR(255),
    holder_did VARCHAR(255),
    holder_xrpl_address VARCHAR(255),
    credential_status_id VARCHAR(500),
    credential_status_purpose_code VARCHAR(50) NOT NULL DEFAULT 'revocation',
    kyc_level_code VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    jurisdiction_code VARCHAR(10) NOT NULL DEFAULT 'KR',
    credential_salt_hash VARCHAR(255),
    offer_token_hash VARCHAR(255),
    offer_expires_at TIMESTAMP,
    offer_used_yn CHAR(1) DEFAULT 'N',
    vc_format VARCHAR(50),
    CONSTRAINT PK_credentials PRIMARY KEY (credential_id),
    CONSTRAINT FK_credentials_corporates FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT FK_credentials_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT UK_credentials_external_id UNIQUE (credential_external_id)
);

CREATE INDEX IDX_credentials_holder_did
ON credentials (holder_did);

-- Credential Offer 테이블
CREATE TABLE credential_offers (
    credential_offer_id BIGSERIAL PRIMARY KEY,
    kyc_id BIGINT NOT NULL,
    corporate_id BIGINT NOT NULL,
    offer_token_hash VARCHAR(255) NOT NULL,
    offer_status_code VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    credential_id BIGINT NULL,
    device_id VARCHAR(255) NULL,
    holder_did VARCHAR(255) NULL,
    holder_xrpl_address VARCHAR(255) NULL,
    failure_reason_code VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credential_offers_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications (kyc_id),
    CONSTRAINT fk_credential_offers_corporate FOREIGN KEY (corporate_id) REFERENCES corporates (corporate_id),
    CONSTRAINT fk_credential_offers_credential FOREIGN KEY (credential_id) REFERENCES credentials (credential_id)
);

CREATE INDEX idx_credential_offers_kyc_id
ON credential_offers (kyc_id);

CREATE INDEX idx_credential_offers_corporate_id
ON credential_offers (corporate_id);

CREATE INDEX idx_credential_offers_status_expires_at
ON credential_offers (offer_status_code, expires_at);

CREATE INDEX idx_credential_offers_credential_id
ON credential_offers (credential_id);

CREATE INDEX idx_credential_offers_corporate_status
ON credential_offers (corporate_id, offer_status_code);

-- Credential 요청 테이블
CREATE TABLE credential_requests (
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

CREATE INDEX idx_credential_requests_credential_id
ON credential_requests(credential_id);

CREATE INDEX idx_credential_requests_status
ON credential_requests(request_status_code);

-- Credential 상태 이력 테이블
CREATE TABLE credential_status_histories (
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

CREATE INDEX idx_credential_status_histories_credential_id
ON credential_status_histories(credential_id);

-- Verifier API Key 테이블
CREATE TABLE verifier_api_keys (
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

CREATE INDEX idx_verifier_api_keys_verifier_id
ON verifier_api_keys(verifier_id);

-- Verifier Callback 테이블
CREATE TABLE verifier_callbacks (
    callback_id BIGSERIAL PRIMARY KEY,
    verifier_id BIGINT NOT NULL,
    callback_url VARCHAR(500) NOT NULL,
    callback_status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_verifier_callbacks_verifier FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id)
);

CREATE INDEX idx_verifier_callbacks_verifier_id
ON verifier_callbacks(verifier_id);

-- Verifier 로그 테이블
CREATE TABLE verifier_logs (
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

CREATE INDEX idx_verifier_logs_verifier_id
ON verifier_logs(verifier_id);

CREATE INDEX idx_verifier_logs_requested_at
ON verifier_logs(requested_at);

-- VP 검증 이력 테이블
CREATE TABLE vp_verifications (
    vp_verification_id BIGSERIAL,
    credential_id BIGINT,
    corporate_id BIGINT,
    request_nonce VARCHAR(255) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    vp_verification_status_code VARCHAR(50) NOT NULL,
    replay_suspected_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    result_summary TEXT,
    requested_at TIMESTAMP NOT NULL,
    presented_at TIMESTAMP,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    vp_request_id VARCHAR(255),
    requester_name VARCHAR(255),
    required_claims_json TEXT,
    challenge VARCHAR(255),
    vp_jwt_hash VARCHAR(255),
    core_request_id VARCHAR(255),
    verifier_id BIGINT,
    finance_institution_code VARCHAR(50),
    request_type_code VARCHAR(30),
    test_yn CHAR(1) DEFAULT 'N',
    re_auth_yn CHAR(1) DEFAULT 'N',
    permission_result_json TEXT,
    callback_status_code VARCHAR(30),
    callback_sent_at TIMESTAMP,
    qr_token_hash VARCHAR(255),
    browser_session_hash VARCHAR(255),
    login_completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_vp_verifications PRIMARY KEY (vp_verification_id),
    CONSTRAINT FK_vp_credentials FOREIGN KEY (credential_id) REFERENCES credentials(credential_id),
    CONSTRAINT FK_vp_corporates FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT FK_vp_verifications_core_requests FOREIGN KEY (core_request_id) REFERENCES core_requests(core_request_id),
    CONSTRAINT fk_vp_verifications_verifier FOREIGN KEY (verifier_id) REFERENCES verifiers(verifier_id),
    CONSTRAINT UK_vp_nonce UNIQUE (request_nonce),
    CONSTRAINT UK_vp_verifications_request_id UNIQUE (vp_request_id)
);

CREATE INDEX IDX_vp_verifications_request_id
ON vp_verifications (vp_request_id);

CREATE UNIQUE INDEX uk_vp_verifications_qr_token_hash
ON vp_verifications (qr_token_hash)
WHERE qr_token_hash IS NOT NULL;

CREATE INDEX idx_vp_verifications_status_expires
ON vp_verifications (vp_verification_status_code, expires_at);

-- DID 기관 매핑 테이블
CREATE TABLE did_institutions (
    did VARCHAR(255) PRIMARY KEY,
    institution_name VARCHAR(200) NOT NULL,
    status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_did_institutions_status
ON did_institutions (status_code);
