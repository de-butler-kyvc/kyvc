package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Issuer 정책 반려 DTO */
@Schema(description = "Issuer 정책 반려")
public record IssuerPolicyRejectRequest(
        /** MFA 검증 토큰 */
        @NotBlank
        @Schema(description = "MFA 검증 토큰", example = "mfa_session_token")
        String mfaToken,

        /** 반려 사유 */
        @NotBlank
        @Schema(description = "반려 사유", example = "Issuer 검증 기준 미충족")
        String reason
) {
}
