-- V4: 인증, MFA, 모바일 기기, 모바일 보안, 감사로그, 알림, 동의 테이블 생성 마이그레이션
-- 정의 테이블: auth_tokens, mfa_email_verifications, mobile_device_bindings, mobile_security_settings, audit_logs, notifications, notification_templates, user_consents

-- 인증 토큰 관리 테이블
CREATE TABLE auth_tokens (
    auth_token_id BIGSERIAL,
    actor_type_code VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    token_type_code VARCHAR(50) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_jti VARCHAR(255),
    token_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_auth_tokens PRIMARY KEY (auth_token_id),
    CONSTRAINT UK_auth_tokens_hash UNIQUE (token_hash),
    CONSTRAINT UK_auth_tokens_jti UNIQUE (token_jti)
);

CREATE INDEX IDX_auth_tokens_actor
ON auth_tokens (actor_type_code, actor_id);

CREATE INDEX IDX_auth_tokens_status
ON auth_tokens (token_status_code);

CREATE INDEX IDX_auth_tokens_expires_at
ON auth_tokens (expires_at);

-- 이메일 MFA 검증 테이블
CREATE TABLE mfa_email_verifications (
    mfa_verification_id BIGSERIAL,
    actor_type_code VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    mfa_purpose_code VARCHAR(100) NOT NULL,
    verification_code_hash VARCHAR(255) NOT NULL,
    mfa_status_code VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    failed_attempt_count INTEGER NOT NULL DEFAULT 0,
    requested_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_mfa_email_verifications PRIMARY KEY (mfa_verification_id)
);

CREATE INDEX IDX_mfa_email_verifications_actor
ON mfa_email_verifications (actor_type_code, actor_id);

CREATE INDEX IDX_mfa_email_verifications_status
ON mfa_email_verifications (mfa_status_code);

CREATE INDEX IDX_mfa_email_verifications_expires_at
ON mfa_email_verifications (expires_at);

-- 모바일 기기 바인딩 테이블
CREATE TABLE mobile_device_bindings (
    device_binding_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    os VARCHAR(50),
    app_version VARCHAR(50),
    public_key TEXT,
    device_binding_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    registered_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_mobile_device_bindings PRIMARY KEY (device_binding_id),
    CONSTRAINT FK_mobile_device_bindings_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_mobile_device_bindings_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX IDX_mobile_device_bindings_user
ON mobile_device_bindings (user_id);

-- 모바일 보안 설정 테이블
CREATE TABLE mobile_security_settings (
    security_setting_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    pin_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    biometric_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_mobile_security_settings PRIMARY KEY (security_setting_id),
    CONSTRAINT FK_mobile_security_settings_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_mobile_security_settings_user_device UNIQUE (user_id, device_id)
);

-- 감사로그 테이블
CREATE TABLE audit_logs (
    audit_log_id BIGSERIAL,
    actor_type_code VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    audit_target_type_code VARCHAR(100) NOT NULL,
    target_id BIGINT NOT NULL,
    request_summary TEXT,
    ip_address VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_value_json TEXT,
    after_value_json TEXT,
    CONSTRAINT PK_audit_logs PRIMARY KEY (audit_log_id)
);

CREATE INDEX IDX_audit_logs_actor
ON audit_logs (actor_type_code, actor_id);

CREATE INDEX IDX_audit_logs_target
ON audit_logs (audit_target_type_code, target_id);

CREATE INDEX IDX_audit_logs_created_at
ON audit_logs (created_at);

-- 알림 테이블
CREATE TABLE notifications (
    notification_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    notification_type_code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    channel_code VARCHAR(30),
    target_type_code VARCHAR(30),
    target_id BIGINT,
    template_code VARCHAR(100),
    sent_status_code VARCHAR(30),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT PK_notifications PRIMARY KEY (notification_id),
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IDX_notifications_user_read
ON notifications (user_id, read_yn);

CREATE INDEX idx_notifications_target
ON notifications(target_type_code, target_id);

-- 알림 템플릿 테이블
CREATE TABLE notification_templates (
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

-- 사용자 동의 이력 테이블
CREATE TABLE user_consents (
    consent_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    consent_type_code VARCHAR(100) NOT NULL,
    agreed_yn VARCHAR(1) NOT NULL,
    version VARCHAR(50) NOT NULL,
    agreed_at TIMESTAMP NOT NULL,
    CONSTRAINT PK_user_consents PRIMARY KEY (consent_id),
    CONSTRAINT FK_user_consents_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IDX_user_consents_user_type
ON user_consents (user_id, consent_type_code);
