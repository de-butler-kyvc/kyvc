package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 재발급 요청 DTO
 */
@Schema(description = "VC 재발급 요청")
public record CredentialReissueRequest(
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken, // MFA 검증 토큰

        @Schema(description = "재발급 사유", example = "유효기간 만료에 따른 재발급")
        @NotBlank
        String reason // 재발급 사유
) {
}
