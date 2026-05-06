package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * MFA 검증 응답
 *
 * @param verified 검증 성공 여부
 * @param mfaToken MFA 세션 토큰
 */
@Schema(description = "MFA 검증 응답")
public record MfaVerifyResponse(
        @Schema(description = "검증 성공 여부", example = "true")
        Boolean verified, // 검증 성공 여부
        @Schema(description = "MFA 세션 토큰", example = "mfa-session-token")
        String mfaToken // MFA 세션 토큰
) {
}
