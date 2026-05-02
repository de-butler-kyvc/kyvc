-- V2: KYC 신청, 제출문서, 보완요청, 심사이력 테이블 생성 마이그레이션
-- 정의 테이블: kyc_applications, kyc_documents, kyc_supplements, kyc_review_histories, kyc_supplement_documents

-- KYC 신청 테이블
CREATE TABLE kyc_applications (
    kyc_id BIGSERIAL, -- KYC 신청 ID
    corporate_id BIGINT NOT NULL, -- 법인 ID
    applicant_user_id BIGINT NOT NULL, -- 신청 사용자 ID
    corporate_type_code VARCHAR(50) NOT NULL, -- 법인 유형 코드
    kyc_status_code VARCHAR(50) NOT NULL, -- KYC 신청 상태 코드
    original_document_store_option_code VARCHAR(50), -- 원본서류 저장 옵션 코드
    ai_review_status_code VARCHAR(50), -- AI 심사 상태 코드
    ai_review_result_code VARCHAR(50), -- AI 심사 결과 코드
    ai_confidence_score NUMERIC(5,2), -- AI 신뢰도 점수
    ai_review_summary TEXT, -- AI 심사 요약
    ai_review_detail_json TEXT, -- AI 상세 결과 JSON
    manual_review_reason TEXT, -- 수동심사 사유
    reject_reason TEXT, -- 반려 사유
    submitted_at TIMESTAMP, -- 제출일시
    approved_at TIMESTAMP, -- 승인일시
    rejected_at TIMESTAMP, -- 반려일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    applied_ai_policy_id BIGINT, -- 적용된 AI 심사 정책 ID
    ai_review_reason_code VARCHAR(100), -- 수기검토 사유 코드
    reject_reason_code VARCHAR(100), -- 반려 사유 코드
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

-- KYC 제출서류 테이블
CREATE TABLE kyc_documents (
    document_id BIGSERIAL, -- 문서 ID
    kyc_id BIGINT NOT NULL, -- KYC 신청 ID
    document_type_code VARCHAR(100) NOT NULL, -- 문서 유형 코드
    file_name VARCHAR(255) NOT NULL, -- 원본 파일명
    file_path VARCHAR(1000) NOT NULL, -- 저장 경로
    mime_type VARCHAR(100) NOT NULL, -- MIME 타입
    file_size BIGINT NOT NULL, -- 파일 크기
    document_hash VARCHAR(255) NOT NULL, -- 문서 해시
    upload_status_code VARCHAR(50) NOT NULL, -- 업로드 상태 코드
    uploaded_at TIMESTAMP NOT NULL, -- 업로드일시
    CONSTRAINT PK_kyc_documents PRIMARY KEY (document_id),
    CONSTRAINT FK_kyc_documents_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id)
);

CREATE INDEX IDX_kyc_documents_kyc_type
ON kyc_documents (kyc_id, document_type_code);

-- KYC 보완요청 테이블
CREATE TABLE kyc_supplements (
    supplement_id BIGSERIAL, -- 보완요청 ID
    kyc_id BIGINT NOT NULL, -- KYC 신청 ID
    requested_by_admin_id BIGINT NOT NULL, -- 요청 관리자 ID
    supplement_status_code VARCHAR(50) NOT NULL, -- 보완요청 상태 코드
    request_reason TEXT NOT NULL, -- 보완요청 사유
    requested_document_type_codes TEXT NOT NULL, -- 요청 문서 유형 코드 목록
    requested_at TIMESTAMP NOT NULL, -- 요청일시
    completed_at TIMESTAMP, -- 완료일시
    supplement_reason_code VARCHAR(100), -- 보완 사유 코드
    title VARCHAR(255), -- 보완요청 제목
    message TEXT, -- 보완요청 상세 메시지
    due_at TIMESTAMP, -- 보완 제출 기한
    submitted_comment TEXT, -- 사용자 보완 제출 의견
    CONSTRAINT PK_kyc_supplements PRIMARY KEY (supplement_id),
    CONSTRAINT FK_kyc_supplements_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT FK_kyc_supplements_admin FOREIGN KEY (requested_by_admin_id) REFERENCES admin_users(admin_id)
);

-- KYC 심사 이력 테이블
CREATE TABLE kyc_review_histories (
    review_history_id BIGSERIAL, -- 심사 이력 ID
    kyc_id BIGINT NOT NULL, -- KYC 신청 ID
    admin_id BIGINT NOT NULL, -- 관리자 ID
    review_action_type_code VARCHAR(50) NOT NULL, -- 심사 처리 유형 코드
    before_kyc_status_code VARCHAR(50) NOT NULL, -- 변경 전 KYC 상태 코드
    after_kyc_status_code VARCHAR(50) NOT NULL, -- 변경 후 KYC 상태 코드
    comment TEXT, -- 처리 의견
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    CONSTRAINT PK_kyc_review_histories PRIMARY KEY (review_history_id),
    CONSTRAINT FK_review_histories_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT FK_review_histories_admin FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id)
);

CREATE INDEX IDX_review_histories_kyc
ON kyc_review_histories (kyc_id);

-- KYC 보완요청-문서 매핑 테이블
CREATE TABLE kyc_supplement_documents (
    supplement_document_id BIGSERIAL, -- 보완요청 문서 매핑 ID
    supplement_id BIGINT NOT NULL, -- 보완요청 ID
    document_id BIGINT NOT NULL, -- 문서 ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    CONSTRAINT PK_kyc_supplement_documents PRIMARY KEY (supplement_document_id),
    CONSTRAINT FK_supplement_documents_supplement FOREIGN KEY (supplement_id) REFERENCES kyc_supplements(supplement_id),
    CONSTRAINT FK_supplement_documents_document FOREIGN KEY (document_id) REFERENCES kyc_documents(document_id),
    CONSTRAINT UK_kyc_supplement_documents UNIQUE (supplement_id, document_id)
);

CREATE INDEX IDX_kyc_supplement_documents_supplement
ON kyc_supplement_documents (supplement_id);
