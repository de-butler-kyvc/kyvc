-- V5: 서비스 초기 구동에 필요한 기준 데이터 입력 마이그레이션
-- 대상 테이블: admin_users, admin_roles, admin_user_roles, common_code_groups, common_codes, document_requirements, ai_review_policies, issuer_configs, issuer_policies

-- 관리자 계정 초기 데이터
-- TODO: Replace password_hash with a real bcrypt hash before using this in a real environment.
INSERT INTO admin_users (
    email,
    password_hash,
    name,
    admin_user_status_code
) VALUES (
    'system-admin@kyvc.local',
    '{change-me-bcrypt-hash}',
    '시스템관리자',
    'ACTIVE'
);

-- 관리자 권한 그룹 초기 데이터
INSERT INTO admin_roles (role_code, role_name, description, status) VALUES
    ('BACKEND_ADMIN', '백엔드 관리자', '백엔드 어드민의 KYC 심사, 사용자, VC 발급 상태, Issuer 정책 업무 관리 권한', 'ACTIVE'),
    ('CORE_ADMIN', '코어 관리자', '코어 어드민의 AI, Credential Schema, VC/VP, XRPL, SDK 메타데이터 관리 권한', 'ACTIVE'),
    ('POLICY_MANAGER', '정책 관리자', '공통코드, 문서 정책, AI 심사 정책, Issuer 신뢰정책 관리 권한', 'ACTIVE'),
    ('AUDITOR', '감사담당자', '감사로그와 주요 업무 처리 이력 조회 권한', 'ACTIVE'),
    ('VIEWER', '조회 전용', '관리 화면 조회 전용 권한', 'ACTIVE'),
    ('SYSTEM_ADMIN', '시스템 관리자', '시스템 전체 설정과 관리자 권한을 관리하는 최상위 권한', 'ACTIVE');

-- 관리자-권한 그룹 매핑 초기 데이터
INSERT INTO admin_user_roles (admin_id, role_id)
SELECT au.admin_id, ar.role_id
FROM admin_users au
JOIN admin_roles ar ON ar.role_code = 'SYSTEM_ADMIN'
WHERE au.email = 'system-admin@kyvc.local';

-- 공통 코드 그룹 초기 데이터
INSERT INTO common_code_groups (
    code_group,
    code_group_name,
    description,
    sort_order,
    enabled_yn,
    system_yn,
    created_by_admin_id,
    updated_by_admin_id
) VALUES
    ('DOCUMENT_TYPE', '문서 유형', 'KYC 제출서류 유형 그룹', 1, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATE_TYPE', '법인 유형', 'KYC 신청 법인 유형 그룹', 2, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('SUPPLEMENT_REASON', '보완 사유', 'KYC 보완요청 사유 그룹', 3, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('REJECT_REASON', '반려 사유', 'KYC 반려 사유 그룹', 4, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('AI_REVIEW_REASON', 'AI 수기검토 사유', 'AI 결과 기반 수기검토 사유 그룹', 5, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('AI_REVIEW_ACTION', 'AI 처리 액션', 'AI 정책 처리 액션 그룹', 6, 'Y', 'Y', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'));

-- 공통 코드 상세 초기 데이터
INSERT INTO common_codes (
    code_group_id,
    code,
    code_name,
    description,
    sort_order,
    enabled_yn,
    system_yn,
    metadata_json,
    created_by_admin_id,
    updated_by_admin_id
) VALUES
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'BUSINESS_REGISTRATION', '사업자등록증', '사업자등록증 제출 문서', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'CORPORATE_REGISTRATION', '등기사항전부증명서', '법인 등기 확인 문서', 2, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'SHAREHOLDER_LIST', '주주명부', '주주 및 지분율 확인 문서', 3, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'ARTICLES_OF_INCORPORATION', '정관', '법인 정관 문서', 4, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'POWER_OF_ATTORNEY', '위임장', '대리인 위임 확인 문서', 5, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'REPRESENTATIVE_ID', '대표자 신분확인', '대표자 신분 확인 문서', 6, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'DOCUMENT_TYPE'), 'AGENT_ID', '대리인 신분확인', '대리인 신분 확인 문서', 7, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'CORPORATE_TYPE'), 'CORPORATION', '주식회사', '1차 MVP 지원 법인 유형', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'CORPORATE_TYPE'), 'SOLE_PROPRIETOR', '개인사업자', '후속 확장 법인 유형', 2, 'N', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'DOCUMENT_EXPIRED', '서류 유효기간 만료', '발급일 기준이 맞지 않는 경우', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'SIGNATURE_MISSING', '서명 또는 직인 누락', '서명 또는 직인 확인이 필요한 경우', 2, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'MISSING_REQUIRED_DOC', '필수서류 누락', '필수 제출서류가 누락된 경우', 3, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'FILE_UNREADABLE', '파일 식별 불가', '파일이 훼손되었거나 판독이 어려운 경우', 4, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'ADDRESS_MISMATCH', '주소 불일치', '법인 주소 정보가 불일치하는 경우', 5, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'REPRESENTATIVE_MISMATCH', '대표자 불일치', '대표자 정보가 불일치하는 경우', 6, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'HASH_CHECK_REQUIRED', '문서 무결성 확인 필요', '문서 해시 또는 무결성 재확인이 필요한 경우', 7, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'DUE_EXPIRED', '보완기한 만료', '보완 제출 기한이 만료된 경우', 8, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'ADMIN_CANCELLED', '관리자 취소', '관리자가 보완요청을 취소한 경우', 9, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'SUPPLEMENT_REASON'), 'ADDITIONAL_CHECK', '추가 확인 필요', '추가 확인이 필요한 경우', 10, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'REJECT_REASON'), 'INFO_MISMATCH', '정보 불일치', '문서 간 법인 정보가 불일치하는 경우', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'REJECT_REASON'), 'INVALID_DOCUMENT', '유효하지 않은 문서', '제출 문서가 유효하지 않은 경우', 2, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'REJECT_REASON'), 'UNVERIFIABLE_COMPANY', '법인 확인 불가', '법인 정보를 확인할 수 없는 경우', 3, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'REJECT_REASON'), 'UNAUTHORIZED_AGENT', '대리인 권한 불충분', '대리인 권한 확인이 불충분한 경우', 4, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'REJECT_REASON'), 'BENEFICIAL_OWNER_UNCLEAR', '실제소유자 확인 불가', '실제소유자 확인이 불명확한 경우', 5, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'LOW_AI_CONFIDENCE', 'AI 신뢰도 낮음', 'AI 신뢰도가 정책 기준 미만인 경우', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'AI_REVIEW_FAILED', 'AI 심사 실패', 'AI 심사 처리에 실패한 경우', 2, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'MANUAL_APPROVAL_REQUIRED', '관리자 최종 확인 필요', '관리자 최종 확인이 필요한 경우', 3, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'DOCUMENT_MISMATCH', '문서 간 정보 불일치', 'AI가 문서 간 정보 불일치를 탐지한 경우', 4, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'BENEFICIAL_OWNER_ISSUE', '실제소유자 판단 이슈', '실제소유자 판단에 이슈가 있는 경우', 5, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_REASON'), 'DELEGATION_ISSUE', '위임권한 판단 이슈', '위임권한 판단에 이슈가 있는 경우', 6, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_ACTION'), 'PASS', '통과', '조건 발생 시 통과 처리', 1, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_ACTION'), 'MANUAL_REVIEW', '수기검토', '관리자 수기검토 전환', 2, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_ACTION'), 'NEED_SUPPLEMENT', '보완요청', '사용자 보완요청 전환', 3, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_ACTION'), 'REJECT', '반려', '반려 처리 또는 반려 후보', 4, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ((SELECT code_group_id FROM common_code_groups WHERE code_group = 'AI_REVIEW_ACTION'), 'IGNORE', '무시', '조건 발생 시 별도 처리하지 않음', 5, 'Y', 'Y', '{}'::jsonb, (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'));

-- 법인 유형별 제출서류 정책 초기 데이터
INSERT INTO document_requirements (
    corporate_type_code,
    document_type_code,
    required_yn,
    enabled_yn,
    sort_order,
    guide_message,
    created_by_admin_id,
    updated_by_admin_id
) VALUES
    ('CORPORATION', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '사업자등록증을 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'CORPORATE_REGISTRATION', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'SHAREHOLDER_LIST', 'Y', 'Y', 3, '주주명부를 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'ARTICLES_OF_INCORPORATION', 'Y', 'Y', 4, '정관을 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'POWER_OF_ATTORNEY', 'N', 'Y', 5, '대리 신청 시 위임장을 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'REPRESENTATIVE_ID', 'N', 'Y', 6, '필요 시 대표자 신분확인 문서를 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')),
    ('CORPORATION', 'AGENT_ID', 'N', 'Y', 7, '대리 신청 시 대리인 신분확인 문서를 제출해 주세요.', (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'), (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'));

-- AI 심사 정책 초기 데이터
INSERT INTO ai_review_policies (
    policy_name,
    corporate_type_code,
    auto_approve_enabled_yn,
    auto_approve_min_score,
    manual_review_below_score,
    supplement_below_score,
    mismatch_action_code,
    missing_required_field_action_code,
    delegation_issue_action_code,
    beneficial_owner_issue_action_code,
    ai_failure_action_code,
    enabled_yn,
    effective_from,
    effective_to,
    created_by_admin_id,
    updated_by_admin_id
) VALUES (
    '1차 MVP 주식회사 기본 정책',
    'CORPORATION',
    'N',
    90.00,
    80.00,
    70.00,
    'MANUAL_REVIEW',
    'NEED_SUPPLEMENT',
    'MANUAL_REVIEW',
    'MANUAL_REVIEW',
    'MANUAL_REVIEW',
    'Y',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'),
    (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')
);

-- Issuer 발급 설정 초기 데이터
INSERT INTO issuer_configs (
    issuer_did,
    issuer_name,
    issuer_type_code,
    issuer_xrpl_address,
    verification_method_id,
    signing_key_ref,
    cryptosuite,
    credential_type_code,
    credential_schema_id,
    valid_days,
    default_yn,
    issuer_config_status_code,
    created_by_admin_id,
    updated_by_admin_id
) VALUES (
    'did:xrpl:1:rIssuer',
    'KYvC Platform Issuer',
    'PLATFORM',
    'rIssuer',
    'did:xrpl:1:rIssuer#issuer-key-1',
    'KMS_KEY_001',
    'ecdsa-secp256k1-jcs-poc-2026',
    'KYC_CREDENTIAL',
    'KYVC_KYC_V1',
    365,
    'Y',
    'ACTIVE',
    (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local'),
    (SELECT admin_id FROM admin_users WHERE email = 'system-admin@kyvc.local')
);

-- Issuer 신뢰정책 초기 데이터
INSERT INTO issuer_policies (
    issuer_did,
    issuer_name,
    issuer_policy_type_code,
    credential_type_code,
    issuer_policy_status_code,
    reason
) VALUES (
    'did:xrpl:1:rIssuer',
    'KYvC Platform Issuer',
    'WHITELIST',
    'KYC_CREDENTIAL',
    'ACTIVE',
    '1차 MVP 기본 신뢰 Issuer'
);
