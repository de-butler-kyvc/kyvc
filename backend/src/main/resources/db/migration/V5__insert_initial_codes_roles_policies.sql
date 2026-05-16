-- V5: 서비스 초기 구동에 필요한 기준 데이터 입력 마이그레이션
-- 대상 테이블: admin_users, admin_roles, admin_user_roles, roles, common_code_groups, common_codes, document_requirements, ai_review_policies, issuer_configs, issuer_policies

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
    ('SYSTEM_ADMIN', '시스템 관리자', '시스템 전체 설정과 관리자 권한 관리 권한', 'ACTIVE'),
    ('BACKEND_ADMIN', '백엔드 관리자', '백엔드 어드민의 KYC 심사, 사용자, VC 발급 상태, Issuer 정책 업무 관리 권한', 'ACTIVE');

-- 관리자-권한 그룹 매핑 초기 데이터
INSERT INTO admin_user_roles (admin_id, role_id)
SELECT au.admin_id, ar.role_id
FROM admin_users au
JOIN admin_roles ar ON ar.role_code = 'SYSTEM_ADMIN'
WHERE au.email = 'system-admin@kyvc.local';

-- 사용자 역할 초기 데이터
INSERT INTO roles (
    role_code,
    role_name,
    role_type_code,
    enabled_yn,
    sort_order
) VALUES
    ('CORPORATE_USER', '법인 사용자', 'USER', 'Y', 1),
    ('FINANCE_STAFF', '금융사 직원', 'FINANCE', 'Y', 2),
    ('VERIFIER_APP', '외부 Verifier', 'VERIFIER', 'Y', 3);

-- 공통 코드 그룹 초기 데이터
WITH admin_actor AS (
    SELECT admin_id
    FROM admin_users
    WHERE email = 'system-admin@kyvc.local'
    LIMIT 1
)
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
    ('DOCUMENT_TYPE', '문서 유형', 'KYC 제출서류 유형 그룹', 1, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('CORPORATE_TYPE', '법인 유형', 'KYC 신청 법인 유형 그룹', 2, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('SUPPLEMENT_REASON', '보완 사유', 'KYC 보완요청 사유 그룹', 3, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('REJECT_REASON', '반려 사유', 'KYC 반려 사유 그룹', 4, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('AI_REVIEW_REASON', 'AI 수기검토 사유', 'AI 결과 기반 수기검토 사유 그룹', 5, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('AI_REVIEW_ACTION', 'AI 처리 액션', 'AI 정책 처리 액션 그룹', 6, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('ROLE_TYPE', '역할 유형', '사용자 역할 유형 그룹', 101, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('AGENT_AUTHORITY_STATUS', '대리인 권한 상태', '대리인 권한 상태 그룹', 102, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('FINANCE_CUSTOMER_LINK_STATUS', '금융사 고객 연결 상태', '금융사 고객 연결 상태 그룹', 103, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('DOCUMENT_DELETE_REQUEST_STATUS', '문서 삭제 요청 상태', '문서 삭제 요청 상태 그룹', 104, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('CREDENTIAL_REQUEST_TYPE', 'Credential 요청 유형', 'Credential 요청 유형 그룹', 105, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('CREDENTIAL_REQUEST_STATUS', 'Credential 요청 상태', 'Credential 요청 상태 그룹', 106, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('VERIFIER_STATUS', 'Verifier 상태', 'Verifier 상태 그룹', 107, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('VERIFIER_API_KEY_STATUS', 'Verifier API Key 상태', 'Verifier API Key 상태 그룹', 108, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('VERIFIER_CALLBACK_STATUS', 'Verifier Callback 상태', 'Verifier Callback 상태 그룹', 109, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('CALLBACK_DELIVERY_STATUS', 'Callback 전송 상태', 'Callback 전송 상태 그룹', 110, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('VERIFIER_ACTION_TYPE', 'Verifier 행위 유형', 'Verifier 행위 유형 그룹', 111, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('NOTIFICATION_CHANNEL', '알림 채널', '알림 채널 그룹', 112, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('NOTIFICATION_SEND_STATUS', '알림 발송 상태', '알림 발송 상태 그룹', 113, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor)),
    ('APPLICATION_CHANNEL', '신청 채널', 'KYC 신청 채널 그룹', 114, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor));

-- 공통 코드 상세 초기 데이터
WITH code_values(code_group, code, code_name, description, sort_order, enabled_yn) AS (
    VALUES
        ('DOCUMENT_TYPE', 'BUSINESS_REGISTRATION', '사업자등록증', '사업자등록증 제출 문서', 1, 'Y'),
        ('DOCUMENT_TYPE', 'CORPORATE_REGISTRY', '등기사항전부증명서', '등기사항전부증명서 제출 문서', 2, 'Y'),
        ('DOCUMENT_TYPE', 'SHAREHOLDER_REGISTRY', '주주명부', '주주명부 제출 문서', 3, 'Y'),
        ('DOCUMENT_TYPE', 'STOCK_CHANGE_STATEMENT', '주식변동상황명세서', '주식변동상황명세서 제출 문서', 4, 'Y'),
        ('DOCUMENT_TYPE', 'INVESTOR_REGISTRY', '투자자명부', '투자자명부 제출 문서', 5, 'Y'),
        ('DOCUMENT_TYPE', 'MEMBER_REGISTRY', '사원명부', '사원명부 제출 문서', 6, 'Y'),
        ('DOCUMENT_TYPE', 'BOARD_REGISTRY', '임원명부', '임원명부 제출 문서', 7, 'Y'),
        ('DOCUMENT_TYPE', 'ARTICLES_OF_ASSOCIATION', '정관', '정관 제출 문서', 8, 'Y'),
        ('DOCUMENT_TYPE', 'OPERATING_RULES', '운영규정', '운영규정 제출 문서', 9, 'Y'),
        ('DOCUMENT_TYPE', 'REGULATIONS', '규정', '규정 제출 문서', 10, 'Y'),
        ('DOCUMENT_TYPE', 'MEETING_MINUTES', '회의록', '회의록 제출 문서', 11, 'Y'),
        ('DOCUMENT_TYPE', 'OFFICIAL_LETTER', '공문', '공문 제출 문서', 12, 'Y'),
        ('DOCUMENT_TYPE', 'PURPOSE_PROOF_DOCUMENT', '설립목적 증빙서류', '설립허가증 또는 설립목적 증빙 문서', 13, 'Y'),
        ('DOCUMENT_TYPE', 'ORGANIZATION_IDENTITY_CERTIFICATE', '고유번호증', '고유번호증 제출 문서', 14, 'Y'),
        ('DOCUMENT_TYPE', 'INVESTMENT_REGISTRATION_CERTIFICATE', '외국인투자등록증명서', '외국인투자등록증명서 제출 문서', 15, 'Y'),
        ('DOCUMENT_TYPE', 'BENEFICIAL_OWNER_PROOF_DOCUMENT', '실소유자 증빙서류', '실소유자 확인 보완 문서', 16, 'Y'),
        ('DOCUMENT_TYPE', 'POWER_OF_ATTORNEY', '위임장', '대리인 위임 확인 문서', 17, 'Y'),
        ('DOCUMENT_TYPE', 'SEAL_CERTIFICATE', '인감증명서', '인감증명서 제출 문서', 18, 'Y'),
        ('DOCUMENT_TYPE', 'REPRESENTATIVE_PROOF_DOCUMENT', '대표자 확인서류', '대표자 확인 제출 문서', 19, 'Y'),
        ('CORPORATE_TYPE', 'CORPORATION', '주식회사', '사업자등록증 · 등기사항전부증명서 · 주주명부 또는 주식변동상황명세서', 1, 'Y'),
        ('CORPORATE_TYPE', 'LIMITED_COMPANY', '유한회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 2, 'Y'),
        ('CORPORATE_TYPE', 'LIMITED_PARTNERSHIP', '합자회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 3, 'Y'),
        ('CORPORATE_TYPE', 'GENERAL_PARTNERSHIP', '합명회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 4, 'Y'),
        ('CORPORATE_TYPE', 'NON_PROFIT', '비영리법인', '정관 · 설립목적 증빙서류 · 등기사항전부증명서', 5, 'Y'),
        ('CORPORATE_TYPE', 'ASSOCIATION', '조합·단체', '고유번호증 · 대표자 확인서류 · 규약 문서 중 1개', 6, 'Y'),
        ('CORPORATE_TYPE', 'FOREIGN_COMPANY', '외국기업', '국내 사업자등록증 · 등기 · 본국 설립서류 · 외국인투자등록증명서', 7, 'Y'),
        ('CORPORATE_TYPE', 'SOLE_PROPRIETOR', '개인사업자', '후속 확장 회사 유형', 8, 'Y'),
        ('SUPPLEMENT_REASON', 'DOCUMENT_EXPIRED', '서류 유효기간 만료', '발급일 기준이 맞지 않는 경우', 1, 'Y'),
        ('SUPPLEMENT_REASON', 'SIGNATURE_MISSING', '서명 또는 직인 누락', '서명 또는 직인 확인이 필요한 경우', 2, 'Y'),
        ('SUPPLEMENT_REASON', 'MISSING_REQUIRED_DOC', '필수서류 누락', '필수 제출서류가 누락된 경우', 3, 'Y'),
        ('SUPPLEMENT_REASON', 'FILE_UNREADABLE', '파일 식별 불가', '파일이 훼손되었거나 판독이 어려운 경우', 4, 'Y'),
        ('SUPPLEMENT_REASON', 'ADDRESS_MISMATCH', '주소 불일치', '법인 주소 정보가 불일치하는 경우', 5, 'Y'),
        ('SUPPLEMENT_REASON', 'REPRESENTATIVE_MISMATCH', '대표자 불일치', '대표자 정보가 불일치하는 경우', 6, 'Y'),
        ('SUPPLEMENT_REASON', 'HASH_CHECK_REQUIRED', '문서 무결성 확인 필요', '문서 해시 또는 무결성 재확인이 필요한 경우', 7, 'Y'),
        ('SUPPLEMENT_REASON', 'DUE_EXPIRED', '보완기한 만료', '보완 제출 기한이 만료된 경우', 8, 'Y'),
        ('SUPPLEMENT_REASON', 'ADMIN_CANCELLED', '관리자 취소', '관리자가 보완요청을 취소한 경우', 9, 'Y'),
        ('SUPPLEMENT_REASON', 'ADDITIONAL_CHECK', '추가 확인 필요', '추가 확인이 필요한 경우', 10, 'Y'),
        ('REJECT_REASON', 'INFO_MISMATCH', '정보 불일치', '문서 간 법인 정보가 불일치하는 경우', 1, 'Y'),
        ('REJECT_REASON', 'INVALID_DOCUMENT', '유효하지 않은 문서', '제출 문서가 유효하지 않은 경우', 2, 'Y'),
        ('REJECT_REASON', 'UNVERIFIABLE_COMPANY', '법인 확인 불가', '법인 정보를 확인할 수 없는 경우', 3, 'Y'),
        ('REJECT_REASON', 'UNAUTHORIZED_AGENT', '대리인 권한 불충분', '대리인 권한 확인이 불충분한 경우', 4, 'Y'),
        ('REJECT_REASON', 'BENEFICIAL_OWNER_UNCLEAR', '실제소유자 확인 불가', '실제소유자 확인이 불명확한 경우', 5, 'Y'),
        ('AI_REVIEW_REASON', 'LOW_AI_CONFIDENCE', 'AI 신뢰도 낮음', 'AI 신뢰도가 정책 기준 미만인 경우', 1, 'Y'),
        ('AI_REVIEW_REASON', 'AI_REVIEW_FAILED', 'AI 심사 실패', 'AI 심사 처리에 실패한 경우', 2, 'Y'),
        ('AI_REVIEW_REASON', 'MANUAL_APPROVAL_REQUIRED', '관리자 최종 확인 필요', '관리자 최종 확인이 필요한 경우', 3, 'Y'),
        ('AI_REVIEW_REASON', 'DOCUMENT_MISMATCH', '문서 간 정보 불일치', 'AI가 문서 간 정보 불일치를 탐지한 경우', 4, 'Y'),
        ('AI_REVIEW_REASON', 'BENEFICIAL_OWNER_ISSUE', '실제소유자 판단 이슈', '실제소유자 판단에 이슈가 있는 경우', 5, 'Y'),
        ('AI_REVIEW_REASON', 'DELEGATION_ISSUE', '위임권한 판단 이슈', '위임권한 판단에 이슈가 있는 경우', 6, 'Y'),
        ('AI_REVIEW_ACTION', 'PASS', '통과', '조건 발생 시 통과 처리', 1, 'Y'),
        ('AI_REVIEW_ACTION', 'MANUAL_REVIEW', '수기검토', '관리자 수기검토 전환', 2, 'Y'),
        ('AI_REVIEW_ACTION', 'NEED_SUPPLEMENT', '보완요청', '사용자 보완요청 전환', 3, 'Y'),
        ('AI_REVIEW_ACTION', 'REJECT', '반려', '반려 처리 또는 반려 후보', 4, 'Y'),
        ('AI_REVIEW_ACTION', 'IGNORE', '무시', '조건 발생 시 별도 처리하지 않음', 5, 'Y'),
        ('ROLE_TYPE', 'USER', '사용자', '일반 사용자 역할 유형', 1, 'Y'),
        ('ROLE_TYPE', 'FINANCE', '금융사', '금융기관 사용자 역할 유형', 2, 'Y'),
        ('ROLE_TYPE', 'VERIFIER', 'Verifier', '외부 Verifier 역할 유형', 3, 'Y'),
        ('ROLE_TYPE', 'SYSTEM', '시스템', '시스템 역할 유형', 4, 'Y'),
        ('AGENT_AUTHORITY_STATUS', 'ACTIVE', '활성', '대리인 권한 활성 상태', 1, 'Y'),
        ('AGENT_AUTHORITY_STATUS', 'INACTIVE', '비활성', '대리인 권한 비활성 상태', 2, 'Y'),
        ('AGENT_AUTHORITY_STATUS', 'EXPIRED', '만료', '대리인 권한 만료 상태', 3, 'Y'),
        ('AGENT_AUTHORITY_STATUS', 'SUSPENDED', '정지', '대리인 권한 정지 상태', 4, 'Y'),
        ('AGENT_AUTHORITY_STATUS', 'REVOKED', '폐기', '대리인 권한 폐기 상태', 5, 'Y'),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'ACTIVE', '활성', '금융사 고객 연결 활성 상태', 1, 'Y'),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'INACTIVE', '비활성', '금융사 고객 연결 비활성 상태', 2, 'Y'),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'UNLINKED', '연결 해제', '금융사 고객 연결 해제 상태', 3, 'Y'),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'REQUESTED', '요청', '문서 삭제 요청 접수 상태', 1, 'Y'),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'APPROVED', '승인', '문서 삭제 요청 승인 상태', 2, 'Y'),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'REJECTED', '반려', '문서 삭제 요청 반려 상태', 3, 'Y'),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'COMPLETED', '완료', '문서 삭제 완료 상태', 4, 'Y'),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'CANCELLED', '취소', '문서 삭제 요청 취소 상태', 5, 'Y'),
        ('CREDENTIAL_REQUEST_TYPE', 'ISSUE', '발급', 'Credential 발급 요청', 1, 'Y'),
        ('CREDENTIAL_REQUEST_TYPE', 'REVOKE', '폐기', 'Credential 폐기 요청', 2, 'Y'),
        ('CREDENTIAL_REQUEST_TYPE', 'STATUS_CHECK', '상태 확인', 'Credential 상태 확인 요청', 3, 'Y'),
        ('CREDENTIAL_REQUEST_TYPE', 'REISSUE', '재발급', 'Credential 재발급 요청', 4, 'Y'),
        ('CREDENTIAL_REQUEST_STATUS', 'REQUESTED', '요청', 'Credential 요청 접수 상태', 1, 'Y'),
        ('CREDENTIAL_REQUEST_STATUS', 'PROCESSING', '처리 중', 'Credential 요청 처리 중 상태', 2, 'Y'),
        ('CREDENTIAL_REQUEST_STATUS', 'COMPLETED', '완료', 'Credential 요청 완료 상태', 3, 'Y'),
        ('CREDENTIAL_REQUEST_STATUS', 'FAILED', '실패', 'Credential 요청 실패 상태', 4, 'Y'),
        ('CREDENTIAL_REQUEST_STATUS', 'CANCELLED', '취소', 'Credential 요청 취소 상태', 5, 'Y'),
        ('VERIFIER_STATUS', 'PENDING', '대기', 'Verifier 승인 대기 상태', 1, 'Y'),
        ('VERIFIER_STATUS', 'ACTIVE', '활성', 'Verifier 활성 상태', 2, 'Y'),
        ('VERIFIER_STATUS', 'SUSPENDED', '정지', 'Verifier 정지 상태', 3, 'Y'),
        ('VERIFIER_STATUS', 'REJECTED', '반려', 'Verifier 반려 상태', 4, 'Y'),
        ('VERIFIER_STATUS', 'APPROVED', '승인', 'Verifier 승인 완료 상태', 5, 'Y'),
        ('VERIFIER_API_KEY_STATUS', 'ACTIVE', '활성', 'API Key 활성 상태', 1, 'Y'),
        ('VERIFIER_API_KEY_STATUS', 'REVOKED', '폐기', 'API Key 폐기 상태', 2, 'Y'),
        ('VERIFIER_API_KEY_STATUS', 'EXPIRED', '만료', 'API Key 만료 상태', 3, 'Y'),
        ('VERIFIER_API_KEY_STATUS', 'ROTATED', '교체', 'API Key 교체 상태', 4, 'Y'),
        ('VERIFIER_CALLBACK_STATUS', 'ACTIVE', '활성', 'Verifier Callback 활성 상태', 1, 'Y'),
        ('VERIFIER_CALLBACK_STATUS', 'INACTIVE', '비활성', 'Verifier Callback 비활성 상태', 2, 'Y'),
        ('VERIFIER_CALLBACK_STATUS', 'DISABLED', '비활성화', 'Verifier Callback 비활성화 상태', 3, 'Y'),
        ('CALLBACK_DELIVERY_STATUS', 'PENDING', '대기', 'Callback 전송 대기 상태', 1, 'Y'),
        ('CALLBACK_DELIVERY_STATUS', 'SUCCESS', '성공', 'Callback 전송 성공 상태', 2, 'Y'),
        ('CALLBACK_DELIVERY_STATUS', 'FAILED', '실패', 'Callback 전송 실패 상태', 3, 'Y'),
        ('CALLBACK_DELIVERY_STATUS', 'SENT', '발송', 'Callback 발송 완료 상태', 4, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'VP_REQUEST', 'VP 요청', 'Verifier VP 요청 행위', 1, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'VP_VERIFY', 'VP 검증', 'Verifier VP 검증 행위', 2, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'API_KEY_ISSUE', 'API Key 발급', 'Verifier API Key 발급 행위', 3, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'API_CALL', 'API 호출', 'Verifier API 호출 행위', 4, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'POLICY_SYNC', '정책 동기화', 'Verifier 정책 동기화 행위', 5, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'RE_AUTH', '재인증', 'Verifier 재인증 행위', 6, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'TEST_VERIFY', '테스트 검증', 'Verifier 테스트 검증 행위', 7, 'Y'),
        ('VERIFIER_ACTION_TYPE', 'USAGE_EXPORT', '사용량 내보내기', 'Verifier 사용량 내보내기 행위', 8, 'Y'),
        ('NOTIFICATION_CHANNEL', 'IN_APP', '앱 내 알림', '앱 내 알림 채널', 1, 'Y'),
        ('NOTIFICATION_CHANNEL', 'EMAIL', '이메일', '이메일 알림 채널', 2, 'Y'),
        ('NOTIFICATION_CHANNEL', 'WEB', '웹', '웹 알림 채널', 3, 'Y'),
        ('NOTIFICATION_CHANNEL', 'APP_PUSH', '앱 푸시', '앱 푸시 알림 채널', 4, 'Y'),
        ('NOTIFICATION_CHANNEL', 'SMS', 'SMS', 'SMS 알림 채널', 5, 'Y'),
        ('NOTIFICATION_SEND_STATUS', 'PENDING', '대기', '알림 발송 대기 상태', 1, 'Y'),
        ('NOTIFICATION_SEND_STATUS', 'SENT', '발송', '알림 발송 완료 상태', 2, 'Y'),
        ('NOTIFICATION_SEND_STATUS', 'FAILED', '실패', '알림 발송 실패 상태', 3, 'Y'),
        ('NOTIFICATION_SEND_STATUS', 'READY', '준비', '알림 발송 준비 상태', 4, 'Y'),
        ('NOTIFICATION_SEND_STATUS', 'CANCELLED', '취소', '알림 발송 취소 상태', 5, 'Y'),
        ('APPLICATION_CHANNEL', 'WEB', '웹', '웹 신청 채널', 1, 'Y'),
        ('APPLICATION_CHANNEL', 'MOBILE', '모바일', '모바일 신청 채널', 2, 'Y'),
        ('APPLICATION_CHANNEL', 'FINANCE_BRANCH', '금융사 영업점', '금융사 영업점 신청 채널', 3, 'Y'),
        ('APPLICATION_CHANNEL', 'ONLINE', '온라인', '온라인 신청 채널', 4, 'Y'),
        ('APPLICATION_CHANNEL', 'FINANCE_VISIT', '금융사 방문', '금융사 방문 신청 채널', 5, 'Y')
),
admin_actor AS (
    SELECT admin_id
    FROM admin_users
    WHERE email = 'system-admin@kyvc.local'
    LIMIT 1
)
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
)
SELECT
    groups.code_group_id,
    code_values.code,
    code_values.code_name,
    code_values.description,
    code_values.sort_order,
    code_values.enabled_yn,
    'Y',
    '{}'::jsonb,
    (SELECT admin_id FROM admin_actor),
    (SELECT admin_id FROM admin_actor)
FROM code_values
JOIN common_code_groups groups ON groups.code_group = code_values.code_group;

-- 법인 유형별 제출서류 정책 초기 데이터
WITH requirement_values(corporate_type_code, document_type_code, required_yn, enabled_yn, sort_order, guide_message, requirement_group_code, requirement_group_name, min_required_count) AS (
    VALUES
        ('ASSOCIATION', 'ORGANIZATION_IDENTITY_CERTIFICATE', 'Y', 'Y', 1, '고유번호증을 제출해 주세요.', NULL, NULL, NULL),
        ('ASSOCIATION', 'REPRESENTATIVE_PROOF_DOCUMENT', 'Y', 'Y', 2, '대표자 확인서류를 제출해 주세요.', NULL, NULL, NULL),
        ('ASSOCIATION', 'OPERATING_RULES', 'N', 'Y', 3, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('ASSOCIATION', 'REGULATIONS', 'N', 'Y', 4, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('ASSOCIATION', 'ARTICLES_OF_ASSOCIATION', 'N', 'Y', 5, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('ASSOCIATION', 'POWER_OF_ATTORNEY', 'N', 'Y', 6, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('ASSOCIATION', 'SEAL_CERTIFICATE', 'N', 'Y', 7, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'CORPORATE_REGISTRY', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'SHAREHOLDER_REGISTRY', 'N', 'Y', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('CORPORATION', 'STOCK_CHANGE_STATEMENT', 'N', 'Y', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('CORPORATION', 'POWER_OF_ATTORNEY', 'N', 'Y', 5, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'SEAL_CERTIFICATE', 'N', 'Y', 6, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '국내 사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'CORPORATE_REGISTRY', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'PURPOSE_PROOF_DOCUMENT', 'Y', 'Y', 3, '본국 설립서류 계열 문서를 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'INVESTMENT_REGISTRATION_CERTIFICATE', 'Y', 'Y', 4, '외국인투자등록증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'SHAREHOLDER_REGISTRY', 'N', 'Y', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('FOREIGN_COMPANY', 'STOCK_CHANGE_STATEMENT', 'N', 'Y', 6, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('FOREIGN_COMPANY', 'INVESTOR_REGISTRY', 'N', 'Y', 7, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('FOREIGN_COMPANY', 'POWER_OF_ATTORNEY', 'N', 'Y', 8, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('FOREIGN_COMPANY', 'SEAL_CERTIFICATE', 'N', 'Y', 9, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('GENERAL_PARTNERSHIP', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('GENERAL_PARTNERSHIP', 'CORPORATE_REGISTRY', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('GENERAL_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 'Y', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 'Y', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 'Y', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'POWER_OF_ATTORNEY', 'N', 'Y', 6, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('GENERAL_PARTNERSHIP', 'SEAL_CERTIFICATE', 'N', 'Y', 7, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_COMPANY', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_COMPANY', 'CORPORATE_REGISTRY', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_COMPANY', 'INVESTOR_REGISTRY', 'N', 'Y', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_COMPANY', 'MEMBER_REGISTRY', 'N', 'Y', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_COMPANY', 'ARTICLES_OF_ASSOCIATION', 'N', 'Y', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_COMPANY', 'POWER_OF_ATTORNEY', 'N', 'Y', 6, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_COMPANY', 'SEAL_CERTIFICATE', 'N', 'Y', 7, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_PARTNERSHIP', 'BUSINESS_REGISTRATION', 'Y', 'Y', 1, '사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_PARTNERSHIP', 'CORPORATE_REGISTRY', 'Y', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 'Y', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 'Y', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 'Y', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'POWER_OF_ATTORNEY', 'N', 'Y', 6, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_PARTNERSHIP', 'SEAL_CERTIFICATE', 'N', 'Y', 7, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('NON_PROFIT', 'ARTICLES_OF_ASSOCIATION', 'Y', 'Y', 1, '정관을 제출해 주세요.', NULL, NULL, NULL),
        ('NON_PROFIT', 'PURPOSE_PROOF_DOCUMENT', 'Y', 'Y', 2, '설립목적 증빙서류를 제출해 주세요.', NULL, NULL, NULL),
        ('NON_PROFIT', 'CORPORATE_REGISTRY', 'Y', 'Y', 3, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('NON_PROFIT', 'POWER_OF_ATTORNEY', 'N', 'Y', 4, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('NON_PROFIT', 'SEAL_CERTIFICATE', 'N', 'Y', 5, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL)
),
system_admin AS (
    SELECT admin_id
    FROM admin_users
    WHERE email = 'system-admin@kyvc.local'
    LIMIT 1
)
INSERT INTO document_requirements (
    corporate_type_code,
    document_type_code,
    required_yn,
    enabled_yn,
    sort_order,
    guide_message,
    requirement_group_code,
    requirement_group_name,
    min_required_count,
    created_by_admin_id,
    updated_by_admin_id
)
SELECT
    requirement_values.corporate_type_code,
    requirement_values.document_type_code,
    requirement_values.required_yn,
    requirement_values.enabled_yn,
    requirement_values.sort_order,
    requirement_values.guide_message,
    requirement_values.requirement_group_code,
    requirement_values.requirement_group_name,
    requirement_values.min_required_count,
    (SELECT admin_id FROM system_admin),
    (SELECT admin_id FROM system_admin)
FROM requirement_values;

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
