package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /verifier/presentations/challenges 응답 DTO
public record IssuePresentationChallengeApiResponse(
        @JsonProperty("challenge")
        String challenge, // Challenge 값
        @JsonProperty("domain")
        String domain, // Domain 값
        @JsonProperty("expires_at")
        String expiresAtSnakeCase, // 만료 시각 snake_case
        @JsonProperty("nonce")
        String nonce, // Nonce 값
        @JsonProperty("aud")
        String aud, // Aud 값
        @JsonProperty("expiresAt")
        String expiresAtCamelCase, // 만료 시각 camelCase
        @JsonProperty("presentationDefinition")
        Map<String, Object> presentationDefinition // Presentation Definition 객체
) {
}