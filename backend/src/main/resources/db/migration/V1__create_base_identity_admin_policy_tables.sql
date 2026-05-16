-- V1: 기본 사용자, 법인, 관리자, 권한, 공통코드, 정책, Issuer 기준 테이블 생성 마이그레이션
-- 정의 테이블: users, corporates, admin_users, admin_roles, admin_user_roles, common_code_groups, common_codes, document_requirements, ai_review_policies, issuer_configs, issuer_policies, roles, user_roles

-- 사용자 계정 테이블
CREATE TABLE users (
    user_id BIGSERIAL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type_code VARCHAR(50) NOT NULL,
    user_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_name VARCHAR(100),
    phone VARCHAR(30),
    notification_enabled_yn CHAR(1) DEFAULT 'Y',
    mfa_enabled_yn CHAR(1) DEFAULT 'N',
    mfa_type_code VARCHAR(30),
    last_password_changed_at TIMESTAMP,
    onboarding_corporate_name VARCHAR(255),
    CONSTRAINT PK_users PRIMARY KEY (user_id),
    CONSTRAINT UK_users_email UNIQUE (email)
);

-- 법인 기본정보 테이블
CREATE TABLE corporates (
    corporate_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    corporate_name VARCHAR(255) NOT NULL,
    business_registration_no VARCHAR(50) NOT NULL,
    corporate_registration_no VARCHAR(50),
    representative_name VARCHAR(100),
    representative_phone VARCHAR(50),
    representative_email VARCHAR(255),
    agent_name VARCHAR(100),
    agent_phone VARCHAR(50),
    agent_email VARCHAR(255),
    agent_authority_scope VARCHAR(255),
    address VARCHAR(500),
    business_type VARCHAR(100),
    corporate_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    established_date DATE,
    corporate_type_code VARCHAR(50),
    website VARCHAR(500),
    corporate_phone VARCHAR(50),
    CONSTRAINT PK_corporates PRIMARY KEY (corporate_id),
    CONSTRAINT FK_corporates_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_corporates_brn UNIQUE (business_registration_no),
    CONSTRAINT CK_corporates_status CHECK (corporate_status_code IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- 관리자 계정 테이블
CREATE TABLE admin_users (
    admin_id BIGSERIAL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    admin_user_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    password_changed_at TIMESTAMP,
    login_failed_count INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMP,
    last_login_ip VARCHAR(100),
    CONSTRAINT PK_admin_users PRIMARY KEY (admin_id),
    CONSTRAINT UK_admin_users_email UNIQUE (email)
);

-- 관리자 권한 그룹 테이블
CREATE TABLE admin_roles (
    role_id BIGSERIAL,
    role_code VARCHAR(100) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_admin_roles PRIMARY KEY (role_id),
    CONSTRAINT UK_admin_roles_code UNIQUE (role_code)
);

-- 관리자-권한 그룹 매핑 테이블
CREATE TABLE admin_user_roles (
    admin_user_role_id BIGSERIAL,
    admin_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_admin_user_roles PRIMARY KEY (admin_user_role_id),
    CONSTRAINT FK_admin_user_roles_admin FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    CONSTRAINT FK_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES admin_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT UK_admin_user_roles UNIQUE (admin_id, role_id)
);

-- 공통 코드 그룹 테이블
CREATE TABLE common_code_groups (
    code_group_id BIGSERIAL,
    code_group VARCHAR(100) NOT NULL,
    code_group_name VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    system_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_common_code_groups PRIMARY KEY (code_group_id),
    CONSTRAINT UK_common_code_groups_group UNIQUE (code_group),
    CONSTRAINT FK_common_code_groups_created_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT FK_common_code_groups_updated_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT CK_common_code_groups_sort_order_non_negative CHECK (sort_order >= 0)
);

CREATE INDEX IDX_common_code_groups_enabled
ON common_code_groups (enabled_yn);

-- 공통 코드 상세 테이블
CREATE TABLE common_codes (
    code_id BIGSERIAL,
    code_group_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    code_name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    system_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    metadata_json JSONB DEFAULT '{}'::jsonb,
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_common_codes PRIMARY KEY (code_id),
    CONSTRAINT FK_common_codes_group FOREIGN KEY (code_group_id) REFERENCES common_code_groups(code_group_id),
    CONSTRAINT UK_common_codes_group_code UNIQUE (code_group_id, code),
    CONSTRAINT FK_common_codes_created_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT FK_common_codes_updated_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT CK_common_codes_sort_order_non_negative CHECK (sort_order >= 0)
);

CREATE INDEX IDX_common_codes_group
ON common_codes (code_group_id);

CREATE INDEX IDX_common_codes_enabled
ON common_codes (code_group_id, enabled_yn);

-- 법인 유형별 제출서류 정책 테이블
CREATE TABLE document_requirements (
    requirement_id BIGSERIAL,
    corporate_type_code VARCHAR(50) NOT NULL,
    document_type_code VARCHAR(100) NOT NULL,
    required_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    sort_order INTEGER NOT NULL DEFAULT 0,
    guide_message TEXT,
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requirement_group_code VARCHAR(100),
    requirement_group_name VARCHAR(200),
    min_required_count INTEGER,
    CONSTRAINT PK_document_requirements PRIMARY KEY (requirement_id),
    CONSTRAINT UK_document_requirements_type_doc UNIQUE (corporate_type_code, document_type_code),
    CONSTRAINT FK_document_requirements_created_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT FK_document_requirements_updated_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT CK_document_requirements_sort_order_non_negative CHECK (sort_order >= 0)
);

CREATE INDEX IDX_document_requirements_corporate_type
ON document_requirements (corporate_type_code);

CREATE INDEX IDX_document_requirements_enabled
ON document_requirements (corporate_type_code, enabled_yn);

-- AI 심사 정책 테이블
CREATE TABLE ai_review_policies (
    ai_policy_id BIGSERIAL,
    policy_name VARCHAR(255) NOT NULL,
    corporate_type_code VARCHAR(50) NOT NULL,
    auto_approve_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    auto_approve_min_score NUMERIC(5,2) NOT NULL,
    manual_review_below_score NUMERIC(5,2) NOT NULL,
    supplement_below_score NUMERIC(5,2) NOT NULL,
    mismatch_action_code VARCHAR(50) NOT NULL,
    missing_required_field_action_code VARCHAR(50) NOT NULL,
    delegation_issue_action_code VARCHAR(50) NOT NULL,
    beneficial_owner_issue_action_code VARCHAR(50) NOT NULL,
    ai_failure_action_code VARCHAR(50) NOT NULL,
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_ai_review_policies PRIMARY KEY (ai_policy_id),
    CONSTRAINT FK_ai_review_policies_created_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT FK_ai_review_policies_updated_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT CK_ai_review_policies_score_range CHECK (
        auto_approve_min_score BETWEEN 0 AND 100
        AND manual_review_below_score BETWEEN 0 AND 100
        AND supplement_below_score BETWEEN 0 AND 100
    )
);

CREATE INDEX IDX_ai_review_policies_corporate_type
ON ai_review_policies (corporate_type_code);

CREATE INDEX IDX_ai_review_policies_enabled
ON ai_review_policies (corporate_type_code, enabled_yn, effective_from, effective_to);

-- Issuer 발급 설정 테이블
CREATE TABLE issuer_configs (
    issuer_config_id BIGSERIAL,
    issuer_did VARCHAR(255) NOT NULL,
    issuer_name VARCHAR(255) NOT NULL,
    issuer_type_code VARCHAR(50) NOT NULL,
    issuer_xrpl_address VARCHAR(255) NOT NULL,
    verification_method_id VARCHAR(255) NOT NULL,
    signing_key_ref VARCHAR(255) NOT NULL,
    cryptosuite VARCHAR(100) NOT NULL,
    credential_type_code VARCHAR(100) NOT NULL,
    credential_schema_id VARCHAR(255) NOT NULL,
    valid_days INTEGER NOT NULL,
    default_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    issuer_config_status_code VARCHAR(50) NOT NULL,
    created_by_admin_id BIGINT,
    updated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_issuer_configs PRIMARY KEY (issuer_config_id),
    CONSTRAINT UK_issuer_configs_did UNIQUE (issuer_did),
    CONSTRAINT FK_issuer_configs_created_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL,
    CONSTRAINT FK_issuer_configs_updated_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(admin_id) ON DELETE SET NULL
);

CREATE INDEX IDX_issuer_configs_default
ON issuer_configs (credential_type_code, default_yn, issuer_config_status_code);

CREATE INDEX IDX_issuer_configs_status
ON issuer_configs (issuer_config_status_code);

-- Issuer 신뢰정책 테이블
CREATE TABLE issuer_policies (
    issuer_policy_id BIGSERIAL,
    issuer_did VARCHAR(255) NOT NULL,
    issuer_name VARCHAR(255) NOT NULL,
    issuer_policy_type_code VARCHAR(50) NOT NULL,
    credential_type_code VARCHAR(100),
    issuer_policy_status_code VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_issuer_policies PRIMARY KEY (issuer_policy_id)
);

CREATE INDEX IDX_issuer_policies_did
ON issuer_policies (issuer_did);

CREATE INDEX IDX_issuer_policies_type
ON issuer_policies (issuer_policy_type_code, credential_type_code);

-- 사용자 역할 테이블
CREATE TABLE roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    role_type_code VARCHAR(30) NOT NULL,
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 사용자-역할 매핑 테이블
CREATE TABLE user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE INDEX idx_user_roles_user_id
ON user_roles(user_id);
