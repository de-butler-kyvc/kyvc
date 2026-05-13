package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KYC 보완 제출분 처리 완료 응답 DTO입니다.
 */
@Schema(description = "KYC 보완 제출분 처리 완료 응답")
public record KycSupplementCompleteResponse(
        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId,
        /** 보완요청 ID */
        @Schema(description = "보완요청 ID", example = "5")
        Long supplementId,
        /** 처리 완료 여부 */
        @Schema(description = "처리 완료 여부", example = "true")
        Boolean completed,
        /** 변경된 KYC 상태 */
        @Schema(description = "변경된 KYC 상태", example = "MANUAL_REVIEW")
        String kycStatus,
        /** 변경된 보완요청 상태 */
        @Schema(description = "변경된 보완요청 상태", example = "COMPLETED")
        String supplementStatus
) {
}
