-- V1: 기본 사용자, 법인, 관리자, 권한, 공통코드, 정책, Issuer 기준 테이블 생성 마이그레이션
-- 정의 테이블: users, corporates, admin_users, admin_roles, admin_user_roles, common_code_groups, common_codes, document_requirements, ai_review_policies, issuer_configs, issuer_policies

-- 사용자 계정 테이블
CREATE TABLE users (
    user_id BIGSERIAL, -- 사용자 ID
    email VARCHAR(255) NOT NULL, -- 로그인 이메일
    password_hash VARCHAR(255) NOT NULL, -- 비밀번호 해시
    user_type_code VARCHAR(50) NOT NULL, -- 사용자 유형 코드
    user_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 사용자 상태 코드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_users PRIMARY KEY (user_id),
    CONSTRAINT UK_users_email UNIQUE (email)
);

-- 법인 기본정보 테이블
CREATE TABLE corporates (
    corporate_id BIGSERIAL, -- 법인 ID
    user_id BIGINT NOT NULL, -- 사용자 ID
    corporate_name VARCHAR(255) NOT NULL, -- 법인명
    business_registration_no VARCHAR(50) NOT NULL, -- 사업자등록번호
    corporate_registration_no VARCHAR(50), -- 법인등록번호
    representative_name VARCHAR(100) NOT NULL, -- 대표자명
    representative_phone VARCHAR(50), -- 대표자 연락처
    representative_email VARCHAR(255), -- 대표자 이메일
    agent_name VARCHAR(100), -- 대리인명
    agent_phone VARCHAR(50), -- 대리인 연락처
    agent_email VARCHAR(255), -- 대리인 이메일
    agent_authority_scope VARCHAR(255), -- 대리인 권한 범위
    address VARCHAR(500), -- 법인 주소
    business_type VARCHAR(100), -- 업종
    corporate_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 법인 상태 코드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_corporates PRIMARY KEY (corporate_id),
    CONSTRAINT FK_corporates_users FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT UK_corporates_brn UNIQUE (business_registration_no),
    CONSTRAINT CK_corporates_status CHECK (corporate_status_code IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- 관리자 계정 테이블
CREATE TABLE admin_users (
    admin_id BIGSERIAL, -- 관리자 ID
    email VARCHAR(255) NOT NULL, -- 관리자 로그인 이메일
    password_hash VARCHAR(255) NOT NULL, -- 비밀번호 해시
    name VARCHAR(100) NOT NULL, -- 관리자명
    admin_user_status_code VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 관리자 상태 코드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    last_login_at TIMESTAMP, -- 마지막 로그인 일시
    password_changed_at TIMESTAMP, -- 비밀번호 변경 일시
    login_failed_count INTEGER NOT NULL DEFAULT 0, -- 로그인 실패 횟수
    locked_at TIMESTAMP, -- 계정 잠금 일시
    last_login_ip VARCHAR(100), -- 마지막 로그인 IP
    CONSTRAINT PK_admin_users PRIMARY KEY (admin_id),
    CONSTRAINT UK_admin_users_email UNIQUE (email)
);

-- 관리자 권한 그룹 테이블
CREATE TABLE admin_roles (
    role_id BIGSERIAL, -- 권한 그룹 ID
    role_code VARCHAR(100) NOT NULL, -- 권한 코드
    role_name VARCHAR(100) NOT NULL, -- 권한명
    description VARCHAR(500), -- 권한 설명
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 권한 상태
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_admin_roles PRIMARY KEY (role_id),
    CONSTRAINT UK_admin_roles_code UNIQUE (role_code)
);

-- 관리자-권한 그룹 매핑 테이블
CREATE TABLE admin_user_roles (
    admin_user_role_id BIGSERIAL, -- 관리자 권한 매핑 ID
    admin_id BIGINT NOT NULL, -- 관리자 ID
    role_id BIGINT NOT NULL, -- 권한 그룹 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    CONSTRAINT PK_admin_user_roles PRIMARY KEY (admin_user_role_id),
    CONSTRAINT FK_admin_user_roles_admin FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    CONSTRAINT FK_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES admin_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT UK_admin_user_roles UNIQUE (admin_id, role_id)
);

-- 공통 코드 그룹 테이블
CREATE TABLE common_code_groups (
    code_group_id BIGSERIAL, -- 공통코드 그룹 ID
    code_group VARCHAR(100) NOT NULL, -- 코드 그룹 영문명
    code_group_name VARCHAR(100) NOT NULL, -- 코드 그룹 표시명
    description TEXT, -- 코드 그룹 설명
    sort_order INTEGER NOT NULL DEFAULT 0, -- 정렬 순서
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y', -- 사용 여부
    system_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 시스템 필수 그룹 여부
    created_by_admin_id BIGINT, -- 생성 관리자 ID
    updated_by_admin_id BIGINT, -- 수정 관리자 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    code_id BIGSERIAL, -- 코드 ID
    code_group_id BIGINT NOT NULL, -- 공통코드 그룹 ID
    code VARCHAR(100) NOT NULL, -- 코드값
    code_name VARCHAR(255) NOT NULL, -- 코드 표시명
    description TEXT, -- 코드 설명
    sort_order INTEGER NOT NULL DEFAULT 0, -- 정렬 순서
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y', -- 사용 여부
    system_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 시스템 필수 코드 여부
    metadata_json JSONB DEFAULT '{}'::jsonb, -- 확장 메타데이터
    created_by_admin_id BIGINT, -- 생성 관리자 ID
    updated_by_admin_id BIGINT, -- 수정 관리자 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    requirement_id BIGSERIAL, -- 필수서류 정책 ID
    corporate_type_code VARCHAR(50) NOT NULL, -- 법인 유형 코드
    document_type_code VARCHAR(100) NOT NULL, -- 문서 유형 코드
    required_yn VARCHAR(1) NOT NULL DEFAULT 'Y', -- 필수 여부
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y', -- 사용 여부
    sort_order INTEGER NOT NULL DEFAULT 0, -- 표시 순서
    guide_message TEXT, -- 제출 안내 문구
    created_by_admin_id BIGINT, -- 생성 관리자 ID
    updated_by_admin_id BIGINT, -- 수정 관리자 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    ai_policy_id BIGSERIAL, -- AI 심사 정책 ID
    policy_name VARCHAR(255) NOT NULL, -- 정책명
    corporate_type_code VARCHAR(50) NOT NULL, -- 적용 법인 유형 코드
    auto_approve_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 자동 승인 사용 여부
    auto_approve_min_score NUMERIC(5,2) NOT NULL, -- 자동 승인 최소 신뢰도
    manual_review_below_score NUMERIC(5,2) NOT NULL, -- 수기검토 전환 기준 점수
    supplement_below_score NUMERIC(5,2) NOT NULL, -- 보완요청 후보 기준 점수
    mismatch_action_code VARCHAR(50) NOT NULL, -- 문서 간 불일치 시 처리 코드
    missing_required_field_action_code VARCHAR(50) NOT NULL, -- 필수값 누락 시 처리 코드
    delegation_issue_action_code VARCHAR(50) NOT NULL, -- 위임권한 문제 시 처리 코드
    beneficial_owner_issue_action_code VARCHAR(50) NOT NULL, -- 실제소유자 문제 시 처리 코드
    ai_failure_action_code VARCHAR(50) NOT NULL, -- AI 실패 시 처리 코드
    enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y', -- 사용 여부
    effective_from TIMESTAMP NOT NULL, -- 적용 시작일시
    effective_to TIMESTAMP, -- 적용 종료일시
    created_by_admin_id BIGINT, -- 생성 관리자 ID
    updated_by_admin_id BIGINT, -- 수정 관리자 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    issuer_config_id BIGSERIAL, -- Issuer 설정 ID
    issuer_did VARCHAR(255) NOT NULL, -- Issuer DID
    issuer_name VARCHAR(255) NOT NULL, -- Issuer 이름
    issuer_type_code VARCHAR(50) NOT NULL, -- Issuer 유형 코드
    issuer_xrpl_address VARCHAR(255) NOT NULL, -- Issuer XRPL 주소
    verification_method_id VARCHAR(255) NOT NULL, -- Verification Method ID
    signing_key_ref VARCHAR(255) NOT NULL, -- 서명키 참조
    cryptosuite VARCHAR(100) NOT NULL, -- 서명 알고리즘
    credential_type_code VARCHAR(100) NOT NULL, -- 발급 Credential 유형 코드
    credential_schema_id VARCHAR(255) NOT NULL, -- Credential Schema ID
    valid_days INTEGER NOT NULL, -- VC 기본 유효기간 일수
    default_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- 기본 Issuer 여부
    issuer_config_status_code VARCHAR(50) NOT NULL, -- Issuer 설정 상태 코드
    created_by_admin_id BIGINT, -- 생성 관리자 ID
    updated_by_admin_id BIGINT, -- 수정 관리자 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
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
    issuer_policy_id BIGSERIAL, -- Issuer 정책 ID
    issuer_did VARCHAR(255) NOT NULL, -- Issuer DID
    issuer_name VARCHAR(255) NOT NULL, -- Issuer 이름
    issuer_policy_type_code VARCHAR(50) NOT NULL, -- Issuer 정책 유형 코드
    credential_type_code VARCHAR(100), -- Credential 유형 코드
    issuer_policy_status_code VARCHAR(50) NOT NULL, -- Issuer 정책 상태 코드
    reason TEXT, -- 정책 사유
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_issuer_policies PRIMARY KEY (issuer_policy_id)
);

CREATE INDEX IDX_issuer_policies_did
ON issuer_policies (issuer_did);

CREATE INDEX IDX_issuer_policies_type
ON issuer_policies (issuer_policy_type_code, credential_type_code);
