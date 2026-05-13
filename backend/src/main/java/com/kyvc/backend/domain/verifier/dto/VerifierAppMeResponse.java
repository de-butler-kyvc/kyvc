package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Verifier 앱 정보 응답
 *
 * @param verifierId Verifier ID
 * @param verifierName Verifier 이름
 * @param status Verifier 상태
 * @param apiKeyPrefix API Key 표시 prefix
 * @param callbackUrl 활성 callback URL
 * @param createdAt 생성 일시
 * @param lastUsedAt API Key 마지막 사용 일시
 */
@Schema(description = "Verifier 앱 정보 응답")
public record VerifierAppMeResponse(
        @Schema(description = "Verifier ID", example = "1")
        Long verifierId, // Verifier ID
        @Schema(description = "Verifier 이름", example = "Verifier")
        String verifierName, // Verifier 이름
        @Schema(description = "Verifier 상태", example = "ACTIVE")
        String status, // Verifier 상태
        @Schema(description = "API Key 표시 prefix", example = "kyvc_live_****")
        String apiKeyPrefix, // API Key 표시 prefix
        @Schema(description = "활성 callback URL", example = "https://verifier.example.com/callback")
        String callbackUrl, // 활성 callback URL
        @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
        LocalDateTime createdAt, // 생성 일시
        @Schema(description = "API Key 마지막 사용 일시", example = "2026-05-11T10:00:00")
        LocalDateTime lastUsedAt // API Key 마지막 사용 일시
) {
}
