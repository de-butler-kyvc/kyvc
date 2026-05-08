package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core VP Challenge 발급 응답
 *
 * @param challenge challenge 값
 * @param nonce nonce 값
 * @param domain domain 값
 * @param aud aud 값
 * @param expiresAt 만료 시각
 * @param presentationDefinition Presentation Definition 객체
 */
@Schema(description = "Core VP Challenge 발급 응답")
public record CorePresentationChallengeResponse(
        @Schema(description = "challenge 값", example = "challenge-value")
        String challenge, // Challenge 값
        @Schema(description = "nonce 값", example = "nonce-value")
        String nonce, // Nonce 값
        @Schema(description = "domain 값", example = "kyvc-backend")
        String domain, // Domain 값
        @Schema(description = "aud 값", example = "https://dev-api-kyvc.khuoo.synology.me")
        String aud, // Aud 값
        @Schema(description = "만료 시각", example = "2026-05-08T12:05:00")
        LocalDateTime expiresAt, // 만료 시각
        @Schema(description = "Presentation Definition 객체")
        Map<String, Object> presentationDefinition // Presentation Definition 객체
) {
}