package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 원본서류 삭제 요청 승인 DTO입니다.
 */
@Schema(description = "원본서류 삭제 요청 승인 요청")
public record DocumentDeleteRequestApproveRequest(
        /** MFA 인증 토큰 */
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken,

        /** 승인 코멘트 */
        @Schema(description = "승인 코멘트", example = "사용자 요청에 따라 삭제 승인")
        String comment
) {
}
