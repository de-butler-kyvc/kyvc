-- DID 기관 매핑 테이블
CREATE TABLE IF NOT EXISTS did_institutions (
    did VARCHAR(255) PRIMARY KEY,
    institution_name VARCHAR(200) NOT NULL,
    status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_did_institutions_status
ON did_institutions (status_code);

INSERT INTO admin_roles (
    role_code,
    role_name,
    description,
    status,
    created_at,
    updated_at
) VALUES
    (
        'OPERATOR',
        '일반 운영자',
        '일반 운영 업무 권한',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'SYSTEM_ADMIN',
        '시스템 관리자',
        '시스템 전체 설정과 관리자 권한 관리 권한',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (role_code) DO UPDATE
SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

WITH role_mapping AS (
    SELECT
        old_role.role_id AS old_role_id,
        target_role.role_id AS target_role_id
    FROM (
        VALUES
            ('BACKEND_ADMIN', 'SYSTEM_ADMIN'),
            ('CORE_ADMIN', 'SYSTEM_ADMIN'),
            ('POLICY_MANAGER', 'OPERATOR'),
            ('AUDITOR', 'OPERATOR'),
            ('VIEWER', 'OPERATOR')
    ) AS mapping(old_role_code, target_role_code)
    JOIN admin_roles old_role ON old_role.role_code = mapping.old_role_code
    JOIN admin_roles target_role ON target_role.role_code = mapping.target_role_code
),
mapped_admin_user_roles AS (
    SELECT DISTINCT
        admin_user_roles.admin_id,
        role_mapping.target_role_id
    FROM admin_user_roles
    JOIN role_mapping ON role_mapping.old_role_id = admin_user_roles.role_id
)
INSERT INTO admin_user_roles (
    admin_id,
    role_id
)
SELECT
    admin_id,
    target_role_id
FROM mapped_admin_user_roles
ON CONFLICT (admin_id, role_id) DO NOTHING;

DELETE FROM admin_user_roles
WHERE role_id IN (
    SELECT role_id
    FROM admin_roles
    WHERE role_code IN (
        'BACKEND_ADMIN',
        'CORE_ADMIN',
        'POLICY_MANAGER',
        'AUDITOR',
        'VIEWER'
    )
);

UPDATE admin_roles
SET
    status = 'INACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE role_code IN (
    'BACKEND_ADMIN',
    'CORE_ADMIN',
    'POLICY_MANAGER',
    'AUDITOR',
    'VIEWER'
);

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
    'Y',
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
        (
            'LIMITED_COMPANY',
            '유한회사',
            '사업자등록증 · 등기사항전부증명서 · 출자자명부',
            2
        ),
        (
            'NON_PROFIT',
            '비영리법인',
            '정관 · 설립허가증 · 등기사항전부증명서',
            3
        ),
        (
            'ASSOCIATION',
            '조합·단체',
            '규약 · 고유번호증 · 대표자 확인서류',
            4
        ),
        (
            'FOREIGN_COMPANY',
            '외국기업',
            '국내 사업자등록증 · 등기 · 본국 설립서류',
            5
        )
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
