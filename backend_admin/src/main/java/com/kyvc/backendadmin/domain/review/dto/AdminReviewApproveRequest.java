package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KYC 수동심사 승인 요청에 사용하는 DTO입니다.
 */
@Schema(description = "KYC 수동심사 승인 요청")
public record AdminReviewApproveRequest(
        @Schema(description = "MFA_SESSION 원문 토큰", example = "mfa_session_token")
        String mfaToken,
        @Schema(description = "승인 처리 의견", example = "제출 서류와 심사 기준을 충족했습니다.")
        String comment
) {
}
