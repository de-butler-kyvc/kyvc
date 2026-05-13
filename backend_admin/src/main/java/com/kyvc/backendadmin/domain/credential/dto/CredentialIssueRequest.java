package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 발급 요청 DTO입니다.
 */
@Schema(description = "VC 발급 요청")
public record CredentialIssueRequest(

        /** Credential 유형 */
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType,

        /** MFA 세션 토큰 */
        @NotBlank(message = "mfaToken은 필수입니다.")
        @Schema(description = "MFA 세션 토큰", example = "mfa-session-token")
        String mfaToken
) {
}
