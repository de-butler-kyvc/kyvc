-- 레거시 문서 유형 공통코드 비활성화

UPDATE common_codes
SET
    enabled_yn = 'N',
    updated_by_admin_id = (
        SELECT admin_id
        FROM admin_users
        WHERE email = 'system-admin@kyvc.local'
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE code_group_id IN (
        SELECT code_group_id
        FROM common_code_groups
        WHERE code_group = 'DOCUMENT_TYPE'
    )
  AND code IN (
        'CORPORATE_REGISTRATION',
        'SHAREHOLDER_LIST',
        'ARTICLES_OF_INCORPORATION',
        'REPRESENTATIVE_ID',
        'AGENT_ID'
    );
