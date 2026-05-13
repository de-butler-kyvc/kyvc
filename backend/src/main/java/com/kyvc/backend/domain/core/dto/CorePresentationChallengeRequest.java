package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Core VP Challenge 발급 요청
 *
 * @param domain domain 값
 * @param aud aud 값
 * @param definitionId Presentation Definition ID
 * @param format Presentation format
 * @param presentationDefinition Presentation Definition 객체
 */
@Schema(description = "Core VP Challenge 발급 요청")
public record CorePresentationChallengeRequest(
        @Schema(description = "domain 값", example = "kyvc-backend")
        String domain, // Domain 값
        @Schema(description = "aud 값", example = "https://dev-api-kyvc.khuoo.synology.me")
        String aud, // Aud 값
        @Schema(description = "Presentation Definition ID", example = "kr-stock-company-kyc-v1")
        String definitionId, // Presentation Definition ID
        @Schema(description = "Presentation format", example = "dc+sd-jwt")
        String format, // Presentation Format
        @Schema(description = "Presentation Definition 객체")
        Map<String, Object> presentationDefinition // Presentation Definition 객체
) {
}