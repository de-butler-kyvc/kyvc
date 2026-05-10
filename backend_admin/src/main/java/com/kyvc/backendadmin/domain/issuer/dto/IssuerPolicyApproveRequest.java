package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Issuer 정책 승인 DTO */
@Schema(description = "Issuer 정책 승인")
public record IssuerPolicyApproveRequest(
        /** MFA 검증 토큰 */
        @NotBlank
        @Schema(description = "MFA 검증 토큰", example = "mfa_session_token")
        String mfaToken,

        /** 승인 의견 */
        @Schema(description = "승인 의견", example = "정책 승인")
        String comment
) {
}
