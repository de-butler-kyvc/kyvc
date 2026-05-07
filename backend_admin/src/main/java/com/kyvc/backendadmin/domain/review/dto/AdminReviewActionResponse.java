package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 심사 액션 처리 결과를 전달하는 DTO입니다.
 */
@Schema(description = "KYC 심사 액션 처리 응답")
public record AdminReviewActionResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId,
        @Schema(description = "처리 전 KYC 상태", example = "MANUAL_REVIEW")
        String beforeStatus,
        @Schema(description = "처리 후 KYC 상태", example = "APPROVED")
        String afterStatus,
        @Schema(description = "심사 이력 ID", example = "10")
        Long reviewHistoryId,
        @Schema(description = "보완요청 ID", example = "5")
        Long supplementId,
        @Schema(description = "처리 일시")
        LocalDateTime processedAt
) {
}
