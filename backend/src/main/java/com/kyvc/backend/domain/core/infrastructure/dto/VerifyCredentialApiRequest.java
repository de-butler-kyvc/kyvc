package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /verifier/credentials/verify 요청 DTO
public record VerifyCredentialApiRequest(
        @JsonProperty("format")
        String format, // Credential Format
        @JsonProperty("credential")
        Object credential, // Credential 원문
        @JsonProperty("did_documents")
        Map<String, Object> didDocuments, // DID Document 맵
        @JsonProperty("policy")
        Map<String, Object> policy, // Policy 객체
        @JsonProperty("require_status")
        Boolean requireStatus, // 상태 검증 여부
        @JsonProperty("xrpl_json_rpc_url")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @JsonProperty("allow_mainnet")
        Boolean allowMainnet, // mainnet 허용 여부
        @JsonProperty("status_mode")
        String statusMode // 상태 저장 모드
) {
}
