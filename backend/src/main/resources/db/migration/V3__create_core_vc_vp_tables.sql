-- V3: Core 연동, VC Credential, VP 검증 테이블 생성 마이그레이션
-- 정의 테이블: core_requests, credentials, vp_verifications

-- 코어 내부 요청 추적 테이블
CREATE TABLE core_requests (
    core_request_id VARCHAR(255) NOT NULL, -- Core 요청 ID
    core_request_type_code VARCHAR(50) NOT NULL, -- Core 요청 유형 코드
    core_target_type_code VARCHAR(50) NOT NULL, -- Core 요청 대상 유형 코드
    target_id BIGINT NOT NULL, -- 대상 ID
    core_request_status_code VARCHAR(50) NOT NULL DEFAULT 'QUEUED', -- Core 요청 상태 코드
    request_payload_json TEXT, -- 요청 Payload JSON
    response_payload_json TEXT, -- 응답 또는 Callback Payload JSON
    error_message TEXT, -- 오류 메시지
    retry_count INTEGER NOT NULL DEFAULT 0, -- 재시도 횟수
    requested_at TIMESTAMP NOT NULL, -- 요청일시
    completed_at TIMESTAMP, -- 완료일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    CONSTRAINT PK_core_requests PRIMARY KEY (core_request_id)
);

CREATE INDEX IDX_core_requests_target
ON core_requests (core_target_type_code, target_id);

CREATE INDEX IDX_core_requests_status
ON core_requests (core_request_status_code);

CREATE INDEX IDX_core_requests_requested_at
ON core_requests (requested_at);

-- 검증가능자격증명 메타데이터 테이블
CREATE TABLE credentials (
    credential_id BIGSERIAL, -- Credential ID
    corporate_id BIGINT NOT NULL, -- 법인 ID
    kyc_id BIGINT NOT NULL, -- KYC 신청 ID
    credential_external_id VARCHAR(255) NOT NULL, -- 외부 Credential ID
    credential_type_code VARCHAR(100) NOT NULL, -- Credential 유형 코드
    issuer_did VARCHAR(255) NOT NULL, -- 발급자 DID
    credential_status_code VARCHAR(50) NOT NULL, -- Credential 상태 코드
    vc_hash VARCHAR(255), -- VC 해시
    xrpl_tx_hash VARCHAR(255), -- XRPL 트랜잭션 해시
    qr_token VARCHAR(255), -- QR 토큰
    qr_expires_at TIMESTAMP, -- QR 만료일시
    issued_at TIMESTAMP, -- 발급일시
    expires_at TIMESTAMP, -- 만료일시
    revoked_at TIMESTAMP, -- 폐기일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    wallet_saved_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- Wallet 저장 완료 여부
    wallet_saved_at TIMESTAMP, -- Wallet 저장 완료일시
    wallet_device_id VARCHAR(255), -- Wallet 저장 기기 ID
    holder_did VARCHAR(255), -- Holder DID
    holder_xrpl_address VARCHAR(255), -- Holder XRPL 주소
    credential_status_id VARCHAR(500), -- XRPL Credential Status ID
    credential_status_purpose_code VARCHAR(50) NOT NULL DEFAULT 'revocation', -- Credential Status 목적 코드
    kyc_level_code VARCHAR(50) NOT NULL DEFAULT 'BASIC', -- KYC 등급 코드
    jurisdiction_code VARCHAR(10) NOT NULL DEFAULT 'KR', -- 관할 국가/지역 코드
    credential_salt_hash VARCHAR(255), -- Credential Salt 해시
    CONSTRAINT PK_credentials PRIMARY KEY (credential_id),
    CONSTRAINT FK_credentials_corporates FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT FK_credentials_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications(kyc_id),
    CONSTRAINT UK_credentials_external_id UNIQUE (credential_external_id)
);

CREATE INDEX IDX_credentials_holder_did
ON credentials (holder_did);

-- 검증가능프레젠테이션 검증 이력 테이블
CREATE TABLE vp_verifications (
    vp_verification_id BIGSERIAL, -- VP 검증 ID
    credential_id BIGINT NOT NULL, -- Credential ID
    corporate_id BIGINT NOT NULL, -- 법인 ID
    request_nonce VARCHAR(255) NOT NULL, -- VP 요청 nonce
    purpose VARCHAR(255) NOT NULL, -- 제출 목적
    vp_verification_status_code VARCHAR(50) NOT NULL, -- VP 검증 상태 코드
    replay_suspected_yn VARCHAR(1) NOT NULL DEFAULT 'N', -- Replay 의심 여부
    result_summary TEXT, -- 검증 결과 요약
    requested_at TIMESTAMP NOT NULL, -- 요청일시
    presented_at TIMESTAMP, -- 제출일시
    verified_at TIMESTAMP, -- 검증일시
    expires_at TIMESTAMP NOT NULL, -- 만료일시
    vp_request_id VARCHAR(255), -- VP 요청 ID
    requester_name VARCHAR(255), -- 요청 기관 또는 서비스명
    required_claims_json TEXT, -- 요구 Claim JSON
    challenge VARCHAR(255), -- 검증 challenge
    vp_jwt_hash VARCHAR(255), -- VP JWT 해시
    core_request_id VARCHAR(255), -- Core 검증 요청 ID
    CONSTRAINT PK_vp_verifications PRIMARY KEY (vp_verification_id),
    CONSTRAINT FK_vp_credentials FOREIGN KEY (credential_id) REFERENCES credentials(credential_id),
    CONSTRAINT FK_vp_corporates FOREIGN KEY (corporate_id) REFERENCES corporates(corporate_id),
    CONSTRAINT FK_vp_verifications_core_requests FOREIGN KEY (core_request_id) REFERENCES core_requests(core_request_id),
    CONSTRAINT UK_vp_nonce UNIQUE (request_nonce),
    CONSTRAINT UK_vp_verifications_request_id UNIQUE (vp_request_id)
);

CREATE INDEX IDX_vp_verifications_request_id
ON vp_verifications (vp_request_id);
