package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * MFA 설정 변경 요청
 *
 * @param mfaEnabledYn MFA 사용 여부
 * @param mfaToken MFA 검증 세션 토큰
 */
@Schema(description = "MFA 설정 변경 요청")
public record UserMfaUpdateRequest(
        @Schema(description = "MFA 사용 여부", example = "Y")
        @NotBlank(message = "MFA 사용 여부는 필수입니다.")
        String mfaEnabledYn, // MFA 사용 여부
        @Schema(description = "MFA 검증 세션 토큰", example = "mfa-session-token", nullable = true)
        String mfaToken // MFA 검증 세션 토큰
) {
}
