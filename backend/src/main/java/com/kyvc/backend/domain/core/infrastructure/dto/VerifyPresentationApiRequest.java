package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /verifier/presentations/verify 요청 DTO
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyPresentationApiRequest(
        @JsonProperty("format")
        String format, // Presentation Format
        @JsonProperty("presentation")
        Object presentation, // Presentation 원문 또는 객체
        @JsonProperty("did_documents")
        Map<String, Object> didDocuments, // DID document 목록
        @JsonProperty("policy")
        Map<String, Object> policy, // 검증 정책
        @JsonProperty("require_status")
        Boolean requireStatus, // Credential status 검증 여부
        @JsonProperty("xrpl_json_rpc_url")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @JsonProperty("allow_mainnet")
        Boolean allowMainnet, // mainnet 허용 여부
        @JsonProperty("status_mode")
        String statusMode // Credential status 검증 모드
) {
}
