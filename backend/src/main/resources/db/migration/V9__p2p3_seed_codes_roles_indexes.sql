-- V9: P2~P3 공통코드, 기본 역할, 인덱스 보강

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
    ('APPLICATION_CHANNEL', '신청 채널', 'KYC 신청 채널 그룹', 114, 'Y', 'Y', (SELECT admin_id FROM admin_actor), (SELECT admin_id FROM admin_actor))
ON CONFLICT (code_group) DO NOTHING;

WITH code_values(code_group, code, code_name, description, sort_order) AS (
    VALUES
        ('DOCUMENT_TYPE', 'POWER_OF_ATTORNEY', '위임장', '대리인 권한 확인 문서', 5),
        ('DOCUMENT_TYPE', 'REPRESENTATIVE_ID', '대표자 신분증', '대표자 신분 확인 문서', 6),
        ('DOCUMENT_TYPE', 'AGENT_ID', '대리인 신분증', '대리인 신분 확인 문서', 7),
        ('ROLE_TYPE', 'USER', '사용자', '일반 사용자 역할 유형', 1),
        ('ROLE_TYPE', 'FINANCE', '금융사', '금융기관 사용자 역할 유형', 2),
        ('ROLE_TYPE', 'VERIFIER', 'Verifier', '외부 Verifier 역할 유형', 3),
        ('ROLE_TYPE', 'SYSTEM', '시스템', '시스템 역할 유형', 4),
        ('AGENT_AUTHORITY_STATUS', 'ACTIVE', '활성', '대리인 권한 활성 상태', 1),
        ('AGENT_AUTHORITY_STATUS', 'INACTIVE', '비활성', '대리인 권한 비활성 상태', 2),
        ('AGENT_AUTHORITY_STATUS', 'EXPIRED', '만료', '대리인 권한 만료 상태', 3),
        ('AGENT_AUTHORITY_STATUS', 'SUSPENDED', '정지', '대리인 권한 정지 상태', 4),
        ('AGENT_AUTHORITY_STATUS', 'REVOKED', '폐기', '대리인 권한 폐기 상태', 5),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'ACTIVE', '활성', '금융사 고객 연결 활성 상태', 1),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'INACTIVE', '비활성', '금융사 고객 연결 비활성 상태', 2),
        ('FINANCE_CUSTOMER_LINK_STATUS', 'UNLINKED', '연결 해제', '금융사 고객 연결 해제 상태', 3),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'REQUESTED', '요청', '문서 삭제 요청 접수 상태', 1),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'APPROVED', '승인', '문서 삭제 요청 승인 상태', 2),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'REJECTED', '반려', '문서 삭제 요청 반려 상태', 3),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'COMPLETED', '완료', '문서 삭제 완료 상태', 4),
        ('DOCUMENT_DELETE_REQUEST_STATUS', 'CANCELLED', '취소', '문서 삭제 요청 취소 상태', 5),
        ('CREDENTIAL_REQUEST_TYPE', 'ISSUE', '발급', 'Credential 발급 요청', 1),
        ('CREDENTIAL_REQUEST_TYPE', 'REVOKE', '폐기', 'Credential 폐기 요청', 2),
        ('CREDENTIAL_REQUEST_TYPE', 'STATUS_CHECK', '상태 확인', 'Credential 상태 확인 요청', 3),
        ('CREDENTIAL_REQUEST_TYPE', 'REISSUE', '재발급', 'Credential 재발급 요청', 4),
        ('CREDENTIAL_REQUEST_STATUS', 'REQUESTED', '요청', 'Credential 요청 접수 상태', 1),
        ('CREDENTIAL_REQUEST_STATUS', 'PROCESSING', '처리 중', 'Credential 요청 처리 중 상태', 2),
        ('CREDENTIAL_REQUEST_STATUS', 'COMPLETED', '완료', 'Credential 요청 완료 상태', 3),
        ('CREDENTIAL_REQUEST_STATUS', 'FAILED', '실패', 'Credential 요청 실패 상태', 4),
        ('CREDENTIAL_REQUEST_STATUS', 'CANCELLED', '취소', 'Credential 요청 취소 상태', 5),
        ('VERIFIER_STATUS', 'PENDING', '대기', 'Verifier 승인 대기 상태', 1),
        ('VERIFIER_STATUS', 'ACTIVE', '활성', 'Verifier 활성 상태', 2),
        ('VERIFIER_STATUS', 'SUSPENDED', '정지', 'Verifier 정지 상태', 3),
        ('VERIFIER_STATUS', 'REJECTED', '반려', 'Verifier 반려 상태', 4),
        ('VERIFIER_STATUS', 'APPROVED', '승인', 'Verifier 승인 완료 상태', 5),
        ('VERIFIER_API_KEY_STATUS', 'ACTIVE', '활성', 'API Key 활성 상태', 1),
        ('VERIFIER_API_KEY_STATUS', 'REVOKED', '폐기', 'API Key 폐기 상태', 2),
        ('VERIFIER_API_KEY_STATUS', 'EXPIRED', '만료', 'API Key 만료 상태', 3),
        ('VERIFIER_API_KEY_STATUS', 'ROTATED', '교체', 'API Key 교체 상태', 4),
        ('VERIFIER_CALLBACK_STATUS', 'ACTIVE', '활성', 'Verifier Callback 활성 상태', 1),
        ('VERIFIER_CALLBACK_STATUS', 'INACTIVE', '비활성', 'Verifier Callback 비활성 상태', 2),
        ('VERIFIER_CALLBACK_STATUS', 'DISABLED', '비활성화', 'Verifier Callback 비활성화 상태', 3),
        ('CALLBACK_DELIVERY_STATUS', 'PENDING', '대기', 'Callback 전송 대기 상태', 1),
        ('CALLBACK_DELIVERY_STATUS', 'SUCCESS', '성공', 'Callback 전송 성공 상태', 2),
        ('CALLBACK_DELIVERY_STATUS', 'FAILED', '실패', 'Callback 전송 실패 상태', 3),
        ('CALLBACK_DELIVERY_STATUS', 'SENT', '발송', 'Callback 발송 완료 상태', 4),
        ('VERIFIER_ACTION_TYPE', 'VP_REQUEST', 'VP 요청', 'Verifier VP 요청 행위', 1),
        ('VERIFIER_ACTION_TYPE', 'VP_VERIFY', 'VP 검증', 'Verifier VP 검증 행위', 2),
        ('VERIFIER_ACTION_TYPE', 'API_KEY_ISSUE', 'API Key 발급', 'Verifier API Key 발급 행위', 3),
        ('VERIFIER_ACTION_TYPE', 'API_CALL', 'API 호출', 'Verifier API 호출 행위', 4),
        ('VERIFIER_ACTION_TYPE', 'POLICY_SYNC', '정책 동기화', 'Verifier 정책 동기화 행위', 5),
        ('VERIFIER_ACTION_TYPE', 'RE_AUTH', '재인증', 'Verifier 재인증 행위', 6),
        ('VERIFIER_ACTION_TYPE', 'TEST_VERIFY', '테스트 검증', 'Verifier 테스트 검증 행위', 7),
        ('VERIFIER_ACTION_TYPE', 'USAGE_EXPORT', '사용량 내보내기', 'Verifier 사용량 내보내기 행위', 8),
        ('NOTIFICATION_CHANNEL', 'IN_APP', '앱 내 알림', '앱 내 알림 채널', 1),
        ('NOTIFICATION_CHANNEL', 'EMAIL', '이메일', '이메일 알림 채널', 2),
        ('NOTIFICATION_CHANNEL', 'WEB', '웹', '웹 알림 채널', 3),
        ('NOTIFICATION_CHANNEL', 'APP_PUSH', '앱 푸시', '앱 푸시 알림 채널', 4),
        ('NOTIFICATION_CHANNEL', 'SMS', 'SMS', 'SMS 알림 채널', 5),
        ('NOTIFICATION_SEND_STATUS', 'PENDING', '대기', '알림 발송 대기 상태', 1),
        ('NOTIFICATION_SEND_STATUS', 'SENT', '발송', '알림 발송 완료 상태', 2),
        ('NOTIFICATION_SEND_STATUS', 'FAILED', '실패', '알림 발송 실패 상태', 3),
        ('NOTIFICATION_SEND_STATUS', 'READY', '준비', '알림 발송 준비 상태', 4),
        ('NOTIFICATION_SEND_STATUS', 'CANCELLED', '취소', '알림 발송 취소 상태', 5),
        ('APPLICATION_CHANNEL', 'WEB', '웹', '웹 신청 채널', 1),
        ('APPLICATION_CHANNEL', 'MOBILE', '모바일', '모바일 신청 채널', 2),
        ('APPLICATION_CHANNEL', 'FINANCE_BRANCH', '금융사 영업점', '금융사 영업점 신청 채널', 3),
        ('APPLICATION_CHANNEL', 'ONLINE', '온라인', '온라인 신청 채널', 4),
        ('APPLICATION_CHANNEL', 'FINANCE_VISIT', '금융사 방문', '금융사 방문 신청 채널', 5)
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
    'Y',
    'Y',
    '{}'::jsonb,
    (SELECT admin_id FROM admin_actor),
    (SELECT admin_id FROM admin_actor)
FROM code_values
JOIN common_code_groups groups ON groups.code_group = code_values.code_group
ON CONFLICT (code_group_id, code) DO NOTHING;

INSERT INTO roles (
    role_code,
    role_name,
    role_type_code,
    enabled_yn,
    sort_order
) VALUES
    ('CORPORATE_USER', '법인 사용자', 'USER', 'Y', 1),
    ('FINANCE_STAFF', '금융사 직원', 'FINANCE', 'Y', 2),
    ('VERIFIER_APP', '외부 Verifier', 'VERIFIER', 'Y', 3)
ON CONFLICT (role_code) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id
ON user_roles(user_id);

CREATE INDEX IF NOT EXISTS idx_kyc_applications_applicant_status
ON kyc_applications(applicant_user_id, kyc_status_code, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_kyc_applications_finance_customer
ON kyc_applications(finance_institution_code, finance_customer_no);

CREATE INDEX IF NOT EXISTS idx_kyc_documents_kyc_type
ON kyc_documents(kyc_id, document_type_code);

CREATE INDEX IF NOT EXISTS idx_corporate_documents_corporate_type
ON corporate_documents(corporate_id, document_type_code);

CREATE INDEX IF NOT EXISTS idx_corporate_representatives_corporate_id
ON corporate_representatives(corporate_id);

CREATE INDEX IF NOT EXISTS idx_corporate_agents_corporate_id
ON corporate_agents(corporate_id);

CREATE INDEX IF NOT EXISTS idx_finance_corporate_customers_corporate_id
ON finance_corporate_customers(corporate_id);

CREATE INDEX IF NOT EXISTS idx_document_delete_requests_document_id
ON document_delete_requests(document_id);

CREATE INDEX IF NOT EXISTS idx_document_delete_requests_status
ON document_delete_requests(request_status_code);

CREATE INDEX IF NOT EXISTS idx_credential_requests_credential_id
ON credential_requests(credential_id);

CREATE INDEX IF NOT EXISTS idx_credential_requests_status
ON credential_requests(request_status_code);

CREATE INDEX IF NOT EXISTS idx_credential_status_histories_credential_id
ON credential_status_histories(credential_id);

CREATE INDEX IF NOT EXISTS idx_verifier_api_keys_verifier_id
ON verifier_api_keys(verifier_id);

CREATE INDEX IF NOT EXISTS idx_verifier_callbacks_verifier_id
ON verifier_callbacks(verifier_id);

CREATE INDEX IF NOT EXISTS idx_verifier_logs_verifier_id
ON verifier_logs(verifier_id);

CREATE INDEX IF NOT EXISTS idx_verifier_logs_requested_at
ON verifier_logs(requested_at);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
ON notifications(user_id, read_yn);

CREATE INDEX IF NOT EXISTS idx_notifications_target
ON notifications(target_type_code, target_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_target
ON audit_logs(audit_target_type_code, target_id);
