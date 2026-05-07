package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 관리자 MFA challenge 생성 응답 DTO입니다.
 */
@Schema(description = "관리자 MFA challenge 생성 응답")
public record AdminMfaChallengeResponse(
        /** MFA challenge 식별자입니다. */
        @Schema(description = "MFA challenge 식별자입니다.", example = "1")
        String challengeId,

        /** 인증번호 만료 시각입니다. */
        @Schema(description = "인증번호 만료 시각입니다.", example = "2026-05-06T23:59:00")
        LocalDateTime expiresAt,

        /** 마스킹된 발송 대상 이메일입니다. */
        @Schema(description = "마스킹된 발송 대상 이메일입니다.", example = "a***@kyvc.com")
        String maskedTarget
) {
}
