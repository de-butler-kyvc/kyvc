package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /issuer/credentials/kyc 응답 DTO
public record IssueKycCredentialApiResponse(
        @JsonProperty("format")
        String format, // VC format
        @JsonProperty("credential")
        Object credential, // VC 원문 또는 객체
        @JsonProperty("credentialId")
        String credentialId, // Core 외부 Credential ID
        @JsonProperty("credential_type")
        String credentialType, // Credential 유형
        @JsonProperty("vc_core_hash")
        String vcCoreHash, // VC 해시
        @JsonProperty("status")
        Map<String, Object> status, // Credential status 객체
        @JsonProperty("credential_create_transaction")
        Map<String, Object> credentialCreateTransaction, // 발급 트랜잭션 객체
        @JsonProperty("ledger_entry")
        Map<String, Object> ledgerEntry, // Ledger entry 객체
        @JsonProperty("selectiveDisclosure")
        Map<String, Object> selectiveDisclosure, // 선택공개 정보
        @JsonProperty("status_mode")
        String statusMode // 상태 저장 모드
) {
}
