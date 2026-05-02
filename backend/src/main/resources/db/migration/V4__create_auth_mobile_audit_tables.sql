-- V4: 인증, MFA, 모바일 기기, 모바일 보안, 감사로그, 알림, 동의 테이블 생성 마이그레이션
-- 정의 테이블: auth_tokens, mfa_email_verifications, mobile_device_bindings, mobile_security_settings, audit_logs, notifications, user_consents

-- 인증 토큰 관리 테이블
CREATE TABLE auth_tokens (
    auth_token_id BIGSERIAL, -- 인증 토큰 ID
    actor_type_code VARCHAR(50) NOT NULL, -- 행위자 유형 코드
    actor_id BIGINT NOT NULL, -- 행위자 ID
    token_type_code VARCHAR(50) NOT NULL, -- 토큰 유형 코드
    token_hash VARCHAR(255) NOT NULL, -- 토큰 해시
    token_jti VARCHAR(255), -- JWT ID
    token_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 토큰 상태 코드
    issued_at TIMESTAMP NOT NULL, -- 발급일시
    expires_at TIMESTAMP NOT NULL, -- 만료일시
    revoked_at TIMESTAMP, -- 폐기일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    mfa_verification_id BIGSERIAL, -- MFA 검증 ID
    actor_type_code VARCHAR(50) NOT NULL, -- 행위자 유형 코드
    actor_id BIGINT NOT NULL, -- 행위자 ID
    email VARCHAR(255) NOT NULL, -- 인증 대상 이메일
    mfa_purpose_code VARCHAR(100) NOT NULL, -- MFA 인증 목적 코드
    verification_code_hash VARCHAR(255) NOT NULL, -- 인증번호 해시
    mfa_status_code VARCHAR(50) NOT NULL DEFAULT 'REQUESTED', -- MFA 상태 코드
    failed_attempt_count INTEGER NOT NULL DEFAULT 0, -- 실패 횟수
    requested_at TIMESTAMP NOT NULL, -- 요청일시
    verified_at TIMESTAMP, -- 검증일시
    expires_at TIMESTAMP NOT NULL, -- 만료일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    device_binding_id BIGSERIAL, -- 기기 바인딩 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    device_id VARCHAR(255) NOT NULL, -- 모바일 기기 ID
    device_name VARCHAR(255), -- 기기명
    os VARCHAR(50), -- 운영체제
    app_version VARCHAR(50), -- 앱 버전
    public_key TEXT, -- 기기 공개키
    device_binding_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 기기 바인딩 상태 코드
    registered_at TIMESTAMP NOT NULL, -- 등록일시
    last_used_at TIMESTAMP, -- 마지막 사용일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_mobile_device_bindings PRIMARY KEY (device_binding_id),
    CONSTRAINT FK_mobile_device_bindings_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_mobile_device_bindings_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX IDX_mobile_device_bindings_user
ON mobile_device_bindings (user_id);

-- 모바일 보안 설정 테이블
CREATE TABLE mobile_security_settings (
    security_setting_id BIGSERIAL, -- 모바일 보안 설정 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    device_id VARCHAR(255) NOT NULL, -- 모바일 기기 ID
    pin_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- PIN 설정 여부
    biometric_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 생체인증 설정 여부
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_mobile_security_settings PRIMARY KEY (security_setting_id),
    CONSTRAINT FK_mobile_security_settings_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_mobile_security_settings_user_device UNIQUE (user_id, device_id)
);

-- 감사로그 테이블
CREATE TABLE audit_logs (
    audit_log_id BIGSERIAL, -- 감사로그 ID
    actor_type_code VARCHAR(50) NOT NULL, -- 행위자 유형 코드
    actor_id BIGINT NOT NULL, -- 행위자 ID
    action_type VARCHAR(100) NOT NULL, -- 작업 유형
    audit_target_type_code VARCHAR(100) NOT NULL, -- 감사 대상 유형 코드
    target_id BIGINT NOT NULL, -- 대상 ID
    request_summary TEXT, -- 요청 요약
    ip_address VARCHAR(100), -- 요청 IP
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
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
    notification_id BIGSERIAL, -- 알림 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    notification_type_code VARCHAR(100) NOT NULL, -- 알림 유형 코드
    title VARCHAR(255) NOT NULL, -- 알림 제목
    message TEXT NOT NULL, -- 알림 메시지
    read_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 읽음 여부
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    CONSTRAINT PK_notifications PRIMARY KEY (notification_id),
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IDX_notifications_user_read
ON notifications (user_id, read_yn);

-- 사용자 동의 이력 테이블
CREATE TABLE user_consents (
    consent_id BIGSERIAL, -- 동의 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    consent_type_code VARCHAR(100) NOT NULL, -- 동의 유형 코드
    agreed_yn VARCHAR(1) NOT NULL, -- 동의 여부
    version VARCHAR(50) NOT NULL, -- 약관 버전
    agreed_at TIMESTAMP NOT NULL, -- 동의일시
    CONSTRAINT PK_user_consents PRIMARY KEY (consent_id),
    CONSTRAINT FK_user_consents_users FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX IDX_user_consents_user_type
ON user_consents (user_id, consent_type_code);
