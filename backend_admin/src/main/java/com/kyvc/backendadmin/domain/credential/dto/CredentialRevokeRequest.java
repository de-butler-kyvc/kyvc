package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 폐기 요청 DTO
 */
@Schema(description = "VC 폐기 요청")
public record CredentialRevokeRequest(
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken, // MFA 검증 토큰

        @Schema(description = "폐기 사유", example = "법인 정보 변경으로 인한 폐기")
        @NotBlank
        String reason // 폐기 사유
) {
}
