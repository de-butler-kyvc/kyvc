package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

// Core /issuer/credentials/kyc 요청 DTO
public record IssueKycCredentialApiRequest(
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @JsonProperty("holder_did")
        String holderDid, // Holder DID
        @JsonProperty("claims")
        Map<String, Object> claims, // Credential claims
        @JsonProperty("valid_from")
        OffsetDateTime validFrom, // 유효 시작 시각
        @JsonProperty("valid_until")
        OffsetDateTime validUntil, // 유효 종료 시각
        @JsonProperty("persist")
        Boolean persist, // 저장 여부
        @JsonProperty("persist_status")
        Boolean persistStatus, // 상태 저장 여부
        @JsonProperty("mark_status_accepted")
        Boolean markStatusAccepted, // 상태 수락 마킹 여부
        @JsonProperty("status_uri")
        String statusUri, // 상태 URI
        @JsonProperty("xrpl_json_rpc_url")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @JsonProperty("allow_mainnet")
        Boolean allowMainnet, // mainnet 허용 여부
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
