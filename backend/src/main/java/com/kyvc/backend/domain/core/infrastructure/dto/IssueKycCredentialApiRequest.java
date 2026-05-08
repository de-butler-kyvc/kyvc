package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

// Core /issuer/credentials/kyc 요청 DTO
public record IssueKycCredentialApiRequest(
        @JsonProperty("issuer_account")
        String issuerAccount, // Issuer XRPL Account
        @JsonProperty("issuer_did")
        String issuerDid, // Issuer DID
        @JsonProperty("key_id")
        String keyId, // Issuer Verification Method key ID
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @JsonProperty("holder_did")
        String holderDid, // Holder DID
        @JsonProperty("claims")
        Map<String, Object> claims, // Credential claims
        @JsonProperty("valid_from")
        LocalDateTime validFrom, // 유효 시작 시각
        @JsonProperty("valid_until")
        LocalDateTime validUntil, // 유효 종료 시각
        @JsonProperty("persist")
        Boolean persist, // 저장 여부
        @JsonProperty("persist_status")
        Boolean persistStatus, // 상태 저장 여부
        @JsonProperty("mark_status_accepted")
        Boolean markStatusAccepted, // 상태 수락 마킹 여부
        @JsonProperty("store_issuer_did_document")
        Boolean storeIssuerDidDocument, // Issuer DID Document 저장 여부
        @JsonProperty("status_uri")
        String statusUri, // 상태 URI
        @JsonProperty("status_mode")
        String statusMode, // 상태 저장 모드
        @JsonProperty("credential_format")
        String credentialFormat, // Credential 형식
        @JsonProperty("format")
        String format, // VC format
        @JsonProperty("holder_key_id")
        String holderKeyId, // Holder key ID
        @JsonProperty("vct")
        String vct // Verifiable Credential Type
) {
}
