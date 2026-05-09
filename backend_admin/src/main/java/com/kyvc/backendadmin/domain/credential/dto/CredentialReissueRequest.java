package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 재발급 요청 DTO입니다.
 */
@Schema(description = "VC 재발급 요청")
public record CredentialReissueRequest(
        /** MFA 인증 토큰 */
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken,

        /** 재발급 사유 */
        @Schema(description = "재발급 사유", example = "유효기간 만료에 따른 재발급")
        @NotBlank
        String reason,

        /** 관리자 코멘트 */
        @Schema(description = "관리자 코멘트", example = "고객 문의 접수 후 재발급 요청")
        String comment
) {
}
