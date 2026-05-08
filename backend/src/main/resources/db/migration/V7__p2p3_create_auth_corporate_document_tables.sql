-- V7: 사용자 역할, 법인 문서, 대표자, 대리인, 금융사 고객 연결, 문서 삭제 요청 테이블

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    role_type_code VARCHAR(30) NOT NULL,
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    enabled_yn CHAR(1) NOT NULL DEFAULT 'Y',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE TABLE IF NOT EXISTS corporate_documents (
    corporate_document_id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL,
    document_type_code VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    document_hash VARCHAR(255) NOT NULL,
    upload_status_code VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    uploaded_by_type_code VARCHAR(30) NOT NULL DEFAULT 'USER',
    uploaded_by_user_id BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_corporate_documents_corporate FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT fk_corporate_documents_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS corporate_representatives (
    representative_id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL,
    representative_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    nationality_code VARCHAR(30),
    phone VARCHAR(30),
    email VARCHAR(255),
    identity_document_id BIGINT,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_corporate_representatives_corporate UNIQUE (corporate_id),
    CONSTRAINT fk_corporate_representatives_corporate FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT fk_corporate_representatives_identity_document FOREIGN KEY (identity_document_id) REFERENCES corporate_documents(corporate_document_id)
);

CREATE TABLE IF NOT EXISTS corporate_agents (
    agent_id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    agent_birth_date DATE,
    agent_phone VARCHAR(30),
    agent_email VARCHAR(255),
    authority_scope TEXT,
    authority_status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    identity_document_id BIGINT,
    delegation_document_id BIGINT,
    valid_from DATE,
    valid_to DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_corporate_agents_corporate FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT fk_corporate_agents_identity_document FOREIGN KEY (identity_document_id) REFERENCES corporate_documents(corporate_document_id),
    CONSTRAINT fk_corporate_agents_delegation_document FOREIGN KEY (delegation_document_id) REFERENCES corporate_documents(corporate_document_id)
);

CREATE TABLE IF NOT EXISTS finance_corporate_customers (
    finance_customer_id BIGSERIAL PRIMARY KEY,
    finance_institution_code VARCHAR(50) NOT NULL,
    finance_branch_code VARCHAR(50),
    finance_customer_no VARCHAR(100) NOT NULL,
    corporate_id BIGINT NOT NULL,
    linked_by_user_id BIGINT,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_code VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uk_finance_corporate_customers_customer UNIQUE (finance_institution_code, finance_customer_no),
    CONSTRAINT fk_finance_corporate_customers_corporate FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT fk_finance_corporate_customers_user FOREIGN KEY (linked_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS document_delete_requests (
    request_id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    request_status_code VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    request_reason TEXT,
    processed_by_admin_id BIGINT,
    processed_reason TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_document_delete_requests_document FOREIGN KEY (document_id) REFERENCES kyc_documents(document_id),
    CONSTRAINT fk_document_delete_requests_user FOREIGN KEY (requested_by_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_document_delete_requests_admin FOREIGN KEY (processed_by_admin_id) REFERENCES admin_users(admin_id)
);
