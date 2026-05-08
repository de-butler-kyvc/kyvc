package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// Core /verifier/presentations/challenges 요청 DTO
public record IssuePresentationChallengeApiRequest(
        @JsonProperty("domain")
        String domain, // Domain 값
        @JsonProperty("aud")
        String aud, // Aud 값
        @JsonProperty("definitionId")
        String definitionId, // Presentation Definition ID
        @JsonProperty("format")
        String format, // Presentation Format
        @JsonProperty("presentationDefinition")
        Map<String, Object> presentationDefinition // Presentation Definition 객체
) {
}