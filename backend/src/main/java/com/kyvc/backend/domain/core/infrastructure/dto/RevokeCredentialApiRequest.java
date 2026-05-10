package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Core /issuer/credentials/revoke 요청 DTO
public record RevokeCredentialApiRequest(
        @JsonProperty("issuer_account")
        String issuerAccount, // Issuer XRPL Account
        @JsonProperty("issuer_seed")
        String issuerSeed, // Issuer seed
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @JsonProperty("credential_type")
        String credentialType, // Credential 유형
        @JsonProperty("jti")
        String jti, // Core 외부 Credential ID
        @JsonProperty("status_id")
        String statusId, // Credential Status ID
        @JsonProperty("holder_did")
        String holderDid, // Holder DID
        @JsonProperty("issuer_did")
        String issuerDid, // Issuer DID
        @JsonProperty("vct")
        String vct, // Verifiable Credential Type
        @JsonProperty("xrpl_json_rpc_url")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @JsonProperty("allow_mainnet")
        Boolean allowMainnet, // mainnet 허용 여부
        @JsonProperty("status_mode")
        String statusMode // 상태 저장 모드
) {
}
