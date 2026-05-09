package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 원본서류 삭제 요청 반려 DTO입니다.
 */
@Schema(description = "원본서류 삭제 요청 반려 요청")
public record DocumentDeleteRequestRejectRequest(
        /** MFA 인증 토큰 */
        @Schema(description = "MFA 인증 토큰", example = "mfa-session-token")
        @NotBlank
        String mfaToken,

        /** 반려 사유 */
        @Schema(description = "반려 사유", example = "보관 의무 기간이 남아 있습니다.")
        @NotBlank
        String rejectReason,

        /** 관리자 코멘트 */
        @Schema(description = "관리자 코멘트", example = "고객에게 보관 의무 안내 필요")
        String comment
) {
}
