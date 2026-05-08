package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Core /issuer/credentials/revoke 요청 DTO
public record RevokeCredentialApiRequest(
        @JsonProperty("issuer_account")
        String issuerAccount, // Issuer XRPL Account
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @JsonProperty("credential_type")
        String credentialType, // Credential 유형
        @JsonProperty("status_id")
        String statusId, // Credential Status ID
        @JsonProperty("jti")
        String jti, // Core 외부 Credential ID
        @JsonProperty("status_mode")
        String statusMode // 상태 저장 모드
) {
}
