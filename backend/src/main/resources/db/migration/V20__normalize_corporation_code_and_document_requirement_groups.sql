-- 주식회사 표준 코드 및 선택 필수 문서 그룹 보정

ALTER TABLE document_requirements
    ADD COLUMN IF NOT EXISTS requirement_group_code VARCHAR(100);

ALTER TABLE document_requirements
    ADD COLUMN IF NOT EXISTS requirement_group_name VARCHAR(200);

ALTER TABLE document_requirements
    ADD COLUMN IF NOT EXISTS min_required_count INTEGER;

WITH corporate_type_group AS (
    SELECT code_group_id
    FROM common_code_groups
    WHERE code_group = 'CORPORATE_TYPE'
),
system_admin AS (
    SELECT admin_id
    FROM admin_users
    WHERE email = 'system-admin@kyvc.local'
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
    updated_by_admin_id,
    created_at,
    updated_at
)
SELECT
    g.code_group_id,
    'CORPORATION',
    '주식회사',
    '사업자등록증 · 등기사항전부증명서 · 주주명부 또는 주식변동상황명세서',
    1,
    'Y',
    'Y',
    '{}'::jsonb,
    a.admin_id,
    a.admin_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporate_type_group g
CROSS JOIN system_admin a
ON CONFLICT (code_group_id, code) DO UPDATE
SET
    code_name = EXCLUDED.code_name,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    enabled_yn = 'Y',
    system_yn = EXCLUDED.system_yn,
    updated_by_admin_id = EXCLUDED.updated_by_admin_id,
    updated_at = CURRENT_TIMESTAMP;

UPDATE common_codes
SET
    enabled_yn = 'N',
    description = 'CORPORATION 입력 alias',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'JOINT_STOCK_COMPANY'
  AND code_group_id IN (
      SELECT code_group_id
      FROM common_code_groups
      WHERE code_group = 'CORPORATE_TYPE'
  );

UPDATE document_requirements
SET
    enabled_yn = 'N',
    updated_at = CURRENT_TIMESTAMP
WHERE corporate_type_code = 'JOINT_STOCK_COMPANY';

UPDATE document_requirements
SET
    enabled_yn = 'N',
    updated_at = CURRENT_TIMESTAMP
WHERE corporate_type_code = 'CORPORATION'
  AND document_type_code IN (
      'CORPORATE_REGISTRATION',
      'SHAREHOLDER_LIST',
      'ARTICLES_OF_INCORPORATION',
      'REPRESENTATIVE_ID',
      'AGENT_ID'
  );

WITH system_admin AS (
    SELECT admin_id
    FROM admin_users
    WHERE email = 'system-admin@kyvc.local'
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
    updated_by_admin_id,
    created_at,
    updated_at
)
SELECT
    v.corporate_type_code,
    v.document_type_code,
    v.required_yn,
    'Y',
    v.sort_order,
    v.guide_message,
    v.requirement_group_code,
    v.requirement_group_name,
    v.min_required_count,
    a.admin_id,
    a.admin_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM system_admin a
CROSS JOIN (
    VALUES
        ('CORPORATION', 'BUSINESS_REGISTRATION', 'Y', 1, '사업자등록증을 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'SHAREHOLDER_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('CORPORATION', 'STOCK_CHANGE_STATEMENT', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('CORPORATION', 'POWER_OF_ATTORNEY', 'N', 5, '대리 신청 시 위임장을 제출해 주세요.', NULL, NULL, NULL),
        ('CORPORATION', 'SEAL_CERTIFICATE', 'N', 6, '대리 신청 시 인감증명서를 제출해 주세요.', NULL, NULL, NULL),
        ('LIMITED_COMPANY', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_COMPANY', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_COMPANY', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('LIMITED_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('GENERAL_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('ASSOCIATION', 'OPERATING_RULES', 'N', 3, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('ASSOCIATION', 'REGULATIONS', 'N', 4, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('ASSOCIATION', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '규약 문서 중 1개로 제출할 수 있습니다.', 'RULE_DOC', '규약 문서', 1),
        ('FOREIGN_COMPANY', 'SHAREHOLDER_REGISTRY', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('FOREIGN_COMPANY', 'STOCK_CHANGE_STATEMENT', 'N', 6, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1),
        ('FOREIGN_COMPANY', 'INVESTOR_REGISTRY', 'N', 7, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.', 'OWNERSHIP_DOC', '소유구조 확인 문서', 1)
) AS v(corporate_type_code, document_type_code, required_yn, sort_order, guide_message, requirement_group_code, requirement_group_name, min_required_count)
ON CONFLICT (corporate_type_code, document_type_code) DO UPDATE
SET
    required_yn = EXCLUDED.required_yn,
    enabled_yn = EXCLUDED.enabled_yn,
    sort_order = EXCLUDED.sort_order,
    guide_message = EXCLUDED.guide_message,
    requirement_group_code = EXCLUDED.requirement_group_code,
    requirement_group_name = EXCLUDED.requirement_group_name,
    min_required_count = EXCLUDED.min_required_count,
    updated_by_admin_id = EXCLUDED.updated_by_admin_id,
    updated_at = CURRENT_TIMESTAMP;
