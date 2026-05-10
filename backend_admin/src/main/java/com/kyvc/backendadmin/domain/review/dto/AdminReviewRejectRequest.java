package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * KYC 수동심사 반려 요청에 사용하는 DTO입니다.
 */
@Schema(description = "KYC 수동심사 반려 요청")
public record AdminReviewRejectRequest(
        @Schema(description = "MFA_SESSION 원문 토큰", example = "mfa_session_token")
        String mfaToken,
        @Schema(description = "REJECT_REASON 공통코드", example = "INVALID_DOCUMENT")
        @NotBlank
        String rejectReasonCode,
        @Schema(description = "반려 처리 의견", example = "사업자등록증 식별 정보가 신청 정보와 일치하지 않습니다.")
        @NotBlank
        String comment
) {
}
