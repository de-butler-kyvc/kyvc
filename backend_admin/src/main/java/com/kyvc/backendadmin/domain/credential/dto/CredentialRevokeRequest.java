package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 폐기 요청 DTO입니다.
 */
@Schema(description = "VC 폐기 요청")
public record CredentialRevokeRequest(
        /** MFA 인증 토큰 */
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken,

        /** 폐기 사유 */
        @Schema(description = "폐기 사유", example = "법인 정보 변경으로 인한 폐기")
        @NotBlank
        String reason,

        /** 관리자 코멘트 */
        @Schema(description = "관리자 코멘트", example = "새 VC 발급 전 기존 VC 폐기")
        String comment
) {
}
