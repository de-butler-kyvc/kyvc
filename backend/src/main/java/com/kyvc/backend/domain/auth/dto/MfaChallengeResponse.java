package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * MFA challenge 생성 응답
 *
 * @param challengeId MFA challenge ID
 * @param expiresAt 만료 일시
 * @param maskedTarget 마스킹 대상
 */
@Schema(description = "MFA challenge 생성 응답")
public record MfaChallengeResponse(
        @Schema(description = "MFA challenge ID", example = "1")
        String challengeId, // MFA challenge ID
        @Schema(description = "만료 일시", example = "2026-05-05T12:05:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "마스킹 대상", example = "u***@example.com")
        String maskedTarget // 마스킹 대상
) {
}
