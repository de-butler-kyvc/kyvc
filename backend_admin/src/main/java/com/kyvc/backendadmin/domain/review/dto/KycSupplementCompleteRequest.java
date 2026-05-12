package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KYC 보완 제출분 처리 완료 요청 DTO입니다.
 */
@Schema(description = "KYC 보완 제출분 처리 완료 요청")
public record KycSupplementCompleteRequest(
        /** 처리 코멘트 */
        @Schema(description = "처리 코멘트", example = "보완 제출 서류 확인 완료")
        String comment,
        /** 처리 후 KYC 상태 */
        @Schema(description = "처리 후 KYC 상태, 생략 시 MANUAL_REVIEW", example = "MANUAL_REVIEW", allowableValues = {"MANUAL_REVIEW"})
        String nextKycStatus
) {
}
