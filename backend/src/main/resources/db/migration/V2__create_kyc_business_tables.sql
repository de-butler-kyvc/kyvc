-- V2: KYC 신청, 제출문서, 보완요청, 심사이력, 법인 업무 테이블 생성 마이그레이션
-- 정의 테이블: kyc_applications, kyc_documents, kyc_supplements, kyc_review_histories, kyc_supplement_documents, corporate_documents, corporate_representatives, corporate_agents, finance_corporate_customers, document_delete_requests

-- KYC 신청 테이블
CREATE TABLE kyc_applications (
    kyc_id BIGSERIAL,
    corporate_id BIGINT NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    corporate_type_code VARCHAR(50) NOT NULL,
    kyc_status_code VARCHAR(50) NOT NULL,
    original_document_store_option_code VARCHAR(50),
    ai_review_status_code VARCHAR(50),
    ai_review_result_code VARCHAR(50),
    ai_confidence_score NUMERIC(5,2),
    ai_review_summary TEXT,
    ai_review_detail_json TEXT,
    manual_review_reason TEXT,
    reject_reason TEXT,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_ai_policy_id BIGINT,
    ai_review_reason_code VARCHAR(100),
    reject_reason_code VARCHAR(100),
    application_channel_code VARCHAR(30),
    finance_institution_code VARCHAR(50),
    finance_branch_code VARCHAR(50),
    finance_staff_user_id BIGINT,
    finance_customer_no VARCHAR(100),
    visited_at TIMESTAMP,
    core_ai_assessment_json TEXT,
    core_ai_review_raw_json TEXT,
    CONSTRAINT PK_kyc_applications PRIMARY KEY (kyc_id),
    CONSTRAINT FK_kyc_applications_corporates FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT FK_kyc_applications_users FOREIGN KEY (applicant_user_id) REFERENCES users(user_id),
    CONSTRAINT FK_kyc_applications_ai_policy FOREIGN KEY (applied_ai_policy_id) REFERENCES ai_review_policies(ai_policy_id)
);

CREATE INDEX IDX_kyc_applications_status
ON kyc_applications (kyc_status_code);

CREATE INDEX IDX_kyc_applications_ai_policy
ON kyc_applications (applied_ai_policy_id);

CREATE INDEX IDX_kyc_applications_manual_reason
ON kyc_applications (ai_review_reason_code);

CREATE INDEX IDX_kyc_applications_reject_reason
ON kyc_applications (reject_reason_code);

CREATE INDEX idx_kyc_applications_applicant_status
ON kyc_applications(applicant_user_id, kyc_status_code, updated_at DESC);

CREATE INDEX idx_kyc_applications_finance_customer
ON kyc_applications(finance_institution_code, finance_customer_no);

-- KYC 제출서류 테이블
CREATE TABLE kyc_documents (
    document_id BIGSERIAL,
    kyc_id BIGINT NOT NULL,
    document_type_code VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    document_hash VARCHAR(255) NOT NULL,
    upload_status_code VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    uploaded_by_type_code VARCHAR(30),
    uploaded_by_user_id BIGINT,
    CONSTRAINT PK_kyc_documents PRIMARY KEY (document_id),
    CONSTRAINT FK_kyc_documents_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id)
);

CREATE INDEX IDX_kyc_documents_kyc_type
ON kyc_documents (kyc_id, document_type_code);

-- KYC 보완요청 테이블
CREATE TABLE kyc_supplements (
    supplement_id BIGSERIAL,
    kyc_id BIGINT NOT NULL,
    requested_by_admin_id BIGINT NOT NULL,
    supplement_status_code VARCHAR(50) NOT NULL,
    request_reason TEXT NOT NULL,
    requested_document_type_codes TEXT NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    supplement_reason_code VARCHAR(100),
    title VARCHAR(255),
    message TEXT,
    due_at TIMESTAMP,
    submitted_comment TEXT,
    CONSTRAINT PK_kyc_supplements PRIMARY KEY (supplement_id),
    CONSTRAINT FK_kyc_supplements_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT FK_kyc_supplements_admin FOREIGN KEY (requested_by_admin_id) REFERENCES admin_users(admin_id)
);

-- KYC 심사 이력 테이블
CREATE TABLE kyc_review_histories (
    review_history_id BIGSERIAL,
    kyc_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    review_action_type_code VARCHAR(50) NOT NULL,
    before_kyc_status_code VARCHAR(50) NOT NULL,
    after_kyc_status_code VARCHAR(50) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_kyc_review_histories PRIMARY KEY (review_history_id),
    CONSTRAINT FK_review_histories_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT FK_review_histories_admin FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id)
);

CREATE INDEX IDX_review_histories_kyc
ON kyc_review_histories (kyc_id);

-- KYC 보완요청-문서 매핑 테이블
CREATE TABLE kyc_supplement_documents (
    supplement_document_id BIGSERIAL,
    supplement_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_kyc_supplement_documents PRIMARY KEY (supplement_document_id),
    CONSTRAINT FK_supplement_documents_supplement FOREIGN KEY (supplement_id) REFERENCES kyc_supplements(supplement_id),
    CONSTRAINT FK_supplement_documents_document FOREIGN KEY (document_id) REFERENCES kyc_documents(document_id),
    CONSTRAINT UK_kyc_supplement_documents UNIQUE (supplement_id, document_id)
);

CREATE INDEX IDX_kyc_supplement_documents_supplement
ON kyc_supplement_documents (supplement_id);

-- 법인 문서 테이블
CREATE TABLE corporate_documents (
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

CREATE INDEX idx_corporate_documents_corporate_type
ON corporate_documents(corporate_id, document_type_code);

-- 법인 대표자 테이블
CREATE TABLE corporate_representatives (
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

CREATE INDEX idx_corporate_representatives_corporate_id
ON corporate_representatives(corporate_id);

-- 법인 대리인 테이블
CREATE TABLE corporate_agents (
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

CREATE INDEX idx_corporate_agents_corporate_id
ON corporate_agents(corporate_id);

-- 금융사 고객 연결 테이블
CREATE TABLE finance_corporate_customers (
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

CREATE INDEX idx_finance_corporate_customers_corporate_id
ON finance_corporate_customers(corporate_id);

-- 문서 삭제 요청 테이블
CREATE TABLE document_delete_requests (
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

CREATE INDEX idx_document_delete_requests_document_id
ON document_delete_requests(document_id);

CREATE INDEX idx_document_delete_requests_status
ON document_delete_requests(request_status_code);
