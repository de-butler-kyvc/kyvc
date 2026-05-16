-- KYC 회사 유형 및 제출 문서 유형 최신 정책 코드 확장

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
    v.code,
    v.code_name,
    v.description,
    v.sort_order,
    v.enabled_yn,
    'Y',
    '{}'::jsonb,
    a.admin_id,
    a.admin_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporate_type_group g
CROSS JOIN system_admin a
CROSS JOIN (
    VALUES
        ('JOINT_STOCK_COMPANY', '주식회사', '사업자등록증 · 등기사항전부증명서 · 주주명부 또는 주식변동상황명세서', 1, 'Y'),
        ('LIMITED_COMPANY', '유한회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 2, 'Y'),
        ('LIMITED_PARTNERSHIP', '합자회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 3, 'Y'),
        ('GENERAL_PARTNERSHIP', '합명회사', '사업자등록증 · 등기사항전부증명서 · 투자자명부/사원명부/정관 중 1개', 4, 'Y'),
        ('NON_PROFIT', '비영리법인', '정관 · 설립목적 증빙서류 · 등기사항전부증명서', 5, 'Y'),
        ('ASSOCIATION', '조합·단체', '고유번호증 · 대표자 확인서류 · 규약 문서 중 1개', 6, 'Y'),
        ('FOREIGN_COMPANY', '외국기업', '국내 사업자등록증 · 등기 · 본국 설립서류 · 외국인투자등록증명서', 7, 'Y'),
        ('SOLE_PROPRIETOR', '개인사업자', '후속 확장 회사 유형', 8, 'Y')
) AS v(code, code_name, description, sort_order, enabled_yn)
ON CONFLICT (code_group_id, code) DO UPDATE
SET
    code_name = EXCLUDED.code_name,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    enabled_yn = EXCLUDED.enabled_yn,
    system_yn = EXCLUDED.system_yn,
    metadata_json = EXCLUDED.metadata_json,
    updated_by_admin_id = EXCLUDED.updated_by_admin_id,
    updated_at = CURRENT_TIMESTAMP;

WITH document_type_group AS (
    SELECT code_group_id
    FROM common_code_groups
    WHERE code_group = 'DOCUMENT_TYPE'
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
    v.code,
    v.code_name,
    v.description,
    v.sort_order,
    'Y',
    'Y',
    '{}'::jsonb,
    a.admin_id,
    a.admin_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM document_type_group g
CROSS JOIN system_admin a
CROSS JOIN (
    VALUES
        ('BUSINESS_REGISTRATION', '사업자등록증', '사업자등록증 제출 문서', 1),
        ('CORPORATE_REGISTRY', '등기사항전부증명서', '등기사항전부증명서 제출 문서', 2),
        ('SHAREHOLDER_REGISTRY', '주주명부', '주주명부 제출 문서', 3),
        ('STOCK_CHANGE_STATEMENT', '주식변동상황명세서', '주식변동상황명세서 제출 문서', 4),
        ('INVESTOR_REGISTRY', '투자자명부', '투자자명부 제출 문서', 5),
        ('MEMBER_REGISTRY', '사원명부', '사원명부 제출 문서', 6),
        ('BOARD_REGISTRY', '임원명부', '임원명부 제출 문서', 7),
        ('ARTICLES_OF_ASSOCIATION', '정관', '정관 제출 문서', 8),
        ('OPERATING_RULES', '운영규정', '운영규정 제출 문서', 9),
        ('REGULATIONS', '규정', '규정 제출 문서', 10),
        ('MEETING_MINUTES', '회의록', '회의록 제출 문서', 11),
        ('OFFICIAL_LETTER', '공문', '공문 제출 문서', 12),
        ('PURPOSE_PROOF_DOCUMENT', '설립목적 증빙서류', '설립허가증 또는 설립목적 증빙 문서', 13),
        ('ORGANIZATION_IDENTITY_CERTIFICATE', '고유번호증', '고유번호증 제출 문서', 14),
        ('INVESTMENT_REGISTRATION_CERTIFICATE', '외국인투자등록증명서', '외국인투자등록증명서 제출 문서', 15),
        ('BENEFICIAL_OWNER_PROOF_DOCUMENT', '실소유자 증빙서류', '실소유자 확인 보완 문서', 16),
        ('POWER_OF_ATTORNEY', '위임장', '대리인 위임 확인 문서', 17),
        ('SEAL_CERTIFICATE', '인감증명서', '인감증명서 제출 문서', 18),
        ('REPRESENTATIVE_PROOF_DOCUMENT', '대표자 확인서류', '대표자 확인 제출 문서', 19)
) AS v(code, code_name, description, sort_order)
ON CONFLICT (code_group_id, code) DO UPDATE
SET
    code_name = EXCLUDED.code_name,
    description = EXCLUDED.description,
    sort_order = EXCLUDED.sort_order,
    enabled_yn = EXCLUDED.enabled_yn,
    system_yn = EXCLUDED.system_yn,
    metadata_json = EXCLUDED.metadata_json,
    updated_by_admin_id = EXCLUDED.updated_by_admin_id,
    updated_at = CURRENT_TIMESTAMP;

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
    a.admin_id,
    a.admin_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM system_admin a
CROSS JOIN (
    VALUES
        ('JOINT_STOCK_COMPANY', 'BUSINESS_REGISTRATION', 'Y', 1, '사업자등록증을 제출해 주세요.'),
        ('JOINT_STOCK_COMPANY', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.'),
        ('JOINT_STOCK_COMPANY', 'SHAREHOLDER_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('JOINT_STOCK_COMPANY', 'STOCK_CHANGE_STATEMENT', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('JOINT_STOCK_COMPANY', 'POWER_OF_ATTORNEY', 'N', 5, '대리 신청 시 위임장을 제출해 주세요.'),
        ('JOINT_STOCK_COMPANY', 'SEAL_CERTIFICATE', 'N', 6, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('LIMITED_COMPANY', 'BUSINESS_REGISTRATION', 'Y', 1, '사업자등록증을 제출해 주세요.'),
        ('LIMITED_COMPANY', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.'),
        ('LIMITED_COMPANY', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_COMPANY', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_COMPANY', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_COMPANY', 'POWER_OF_ATTORNEY', 'N', 6, '대리 신청 시 위임장을 제출해 주세요.'),
        ('LIMITED_COMPANY', 'SEAL_CERTIFICATE', 'N', 7, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('LIMITED_PARTNERSHIP', 'BUSINESS_REGISTRATION', 'Y', 1, '사업자등록증을 제출해 주세요.'),
        ('LIMITED_PARTNERSHIP', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.'),
        ('LIMITED_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('LIMITED_PARTNERSHIP', 'POWER_OF_ATTORNEY', 'N', 6, '대리 신청 시 위임장을 제출해 주세요.'),
        ('LIMITED_PARTNERSHIP', 'SEAL_CERTIFICATE', 'N', 7, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('GENERAL_PARTNERSHIP', 'BUSINESS_REGISTRATION', 'Y', 1, '사업자등록증을 제출해 주세요.'),
        ('GENERAL_PARTNERSHIP', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.'),
        ('GENERAL_PARTNERSHIP', 'INVESTOR_REGISTRY', 'N', 3, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('GENERAL_PARTNERSHIP', 'MEMBER_REGISTRY', 'N', 4, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('GENERAL_PARTNERSHIP', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('GENERAL_PARTNERSHIP', 'POWER_OF_ATTORNEY', 'N', 6, '대리 신청 시 위임장을 제출해 주세요.'),
        ('GENERAL_PARTNERSHIP', 'SEAL_CERTIFICATE', 'N', 7, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('NON_PROFIT', 'ARTICLES_OF_ASSOCIATION', 'Y', 1, '정관을 제출해 주세요.'),
        ('NON_PROFIT', 'PURPOSE_PROOF_DOCUMENT', 'Y', 2, '설립목적 증빙서류를 제출해 주세요.'),
        ('NON_PROFIT', 'CORPORATE_REGISTRY', 'Y', 3, '등기사항전부증명서를 제출해 주세요.'),
        ('NON_PROFIT', 'POWER_OF_ATTORNEY', 'N', 4, '대리 신청 시 위임장을 제출해 주세요.'),
        ('NON_PROFIT', 'SEAL_CERTIFICATE', 'N', 5, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('ASSOCIATION', 'ORGANIZATION_IDENTITY_CERTIFICATE', 'Y', 1, '고유번호증을 제출해 주세요.'),
        ('ASSOCIATION', 'REPRESENTATIVE_PROOF_DOCUMENT', 'Y', 2, '대표자 확인서류를 제출해 주세요.'),
        ('ASSOCIATION', 'OPERATING_RULES', 'N', 3, '규약 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('ASSOCIATION', 'REGULATIONS', 'N', 4, '규약 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('ASSOCIATION', 'ARTICLES_OF_ASSOCIATION', 'N', 5, '규약 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('ASSOCIATION', 'POWER_OF_ATTORNEY', 'N', 6, '대리 신청 시 위임장을 제출해 주세요.'),
        ('ASSOCIATION', 'SEAL_CERTIFICATE', 'N', 7, '대리 신청 시 인감증명서를 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'BUSINESS_REGISTRATION', 'Y', 1, '국내 사업자등록증을 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'CORPORATE_REGISTRY', 'Y', 2, '등기사항전부증명서를 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'PURPOSE_PROOF_DOCUMENT', 'Y', 3, '본국 설립서류 계열 문서를 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'INVESTMENT_REGISTRATION_CERTIFICATE', 'Y', 4, '외국인투자등록증명서를 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'SHAREHOLDER_REGISTRY', 'N', 5, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('FOREIGN_COMPANY', 'STOCK_CHANGE_STATEMENT', 'N', 6, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('FOREIGN_COMPANY', 'INVESTOR_REGISTRY', 'N', 7, '소유구조 확인 문서 중 1개로 제출할 수 있습니다.'),
        ('FOREIGN_COMPANY', 'POWER_OF_ATTORNEY', 'N', 8, '대리 신청 시 위임장을 제출해 주세요.'),
        ('FOREIGN_COMPANY', 'SEAL_CERTIFICATE', 'N', 9, '대리 신청 시 인감증명서를 제출해 주세요.')
) AS v(corporate_type_code, document_type_code, required_yn, sort_order, guide_message)
ON CONFLICT (corporate_type_code, document_type_code) DO UPDATE
SET
    required_yn = EXCLUDED.required_yn,
    enabled_yn = EXCLUDED.enabled_yn,
    sort_order = EXCLUDED.sort_order,
    guide_message = EXCLUDED.guide_message,
    updated_by_admin_id = EXCLUDED.updated_by_admin_id,
    updated_at = CURRENT_TIMESTAMP;
