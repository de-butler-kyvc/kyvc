package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /credential-status 응답 DTO
public record CredentialStatusApiResponse(
        @JsonProperty("issuer_account")
        String issuerAccount, // Issuer XRPL Account
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @JsonProperty("credential_type")
        String credentialType, // Credential 유형
        @JsonProperty("found")
        boolean found, // 상태 엔트리 존재 여부
        @JsonProperty("active")
        boolean active, // active 여부
        @JsonProperty("entry")
        Map<String, Object> entry, // 상태 엔트리 객체
        @JsonProperty("checked_at")
        String checkedAt // 조회 시각
) {
}
