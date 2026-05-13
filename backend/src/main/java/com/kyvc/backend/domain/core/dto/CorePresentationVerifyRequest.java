package com.kyvc.backend.domain.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Core Presentation 검증 요청
 *
 * @param format Presentation format
 * @param presentation Presentation 객체
 * @param didDocuments DID document 목록
 * @param policy 검증 정책
 * @param requireStatus Credential status 검증 여부
 * @param xrplJsonRpcUrl XRPL JSON RPC URL
 * @param allowMainnet mainnet 허용 여부
 * @param statusMode Credential status 검증 모드
 */
@Schema(description = "Core Presentation 검증 요청")
public record CorePresentationVerifyRequest(
        @Schema(description = "Presentation format", example = "kyvc-sd-jwt-presentation-v1")
        String format, // Presentation format
        @Schema(description = "Presentation 객체")
        Object presentation, // Presentation 객체
        @JsonProperty("did_documents")
        @Schema(description = "DID document 목록")
        Map<String, Map<String, Object>> didDocuments, // DID document 목록
        @Schema(description = "검증 정책")
        Object policy, // 검증 정책
        @JsonProperty("require_status")
        @Schema(description = "Credential status 검증 여부", example = "true")
        boolean requireStatus, // Credential status 검증 여부
        @JsonProperty("xrpl_json_rpc_url")
        @Schema(description = "XRPL JSON RPC URL")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @JsonProperty("allow_mainnet")
        @Schema(description = "mainnet 허용 여부", example = "false")
        boolean allowMainnet, // mainnet 허용 여부
        @JsonProperty("status_mode")
        @Schema(description = "Credential status 검증 모드", example = "xrpl")
        String statusMode // Credential status 검증 모드
) {
}
