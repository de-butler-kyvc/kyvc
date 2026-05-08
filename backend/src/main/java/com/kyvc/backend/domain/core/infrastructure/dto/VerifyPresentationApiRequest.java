package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Core /verifier/presentations/verify 요청 DTO
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerifyPresentationApiRequest(
        @JsonProperty("format")
        String format, // Presentation Format
        @JsonProperty("aud")
        String aud, // 검증 대상 aud
        @JsonProperty("nonce")
        String nonce, // VP nonce
        @JsonProperty("challenge")
        String challenge, // VP challenge
        @JsonProperty("requiredClaims")
        List<String> requiredClaims, // 필수 Claim 목록
        @JsonProperty("vpJwt")
        String vpJwt, // Legacy VP JWT 원문
        @JsonProperty("presentation")
        String presentation, // Presentation 원문
        @JsonProperty("sdJwtKb")
        String sdJwtKb // SD-JWT KB 원문
) {
}
