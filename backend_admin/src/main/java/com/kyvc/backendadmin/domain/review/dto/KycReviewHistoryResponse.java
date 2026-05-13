package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 심사 이력 조회 응답 DTO입니다.
 */
@Schema(description = "KYC 심사 이력 조회 응답")
public record KycReviewHistoryResponse(

        /** 심사 이력 ID */
        @Schema(description = "심사 이력 ID", example = "1000")
        Long historyId,

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** 심사 처리 유형 코드 */
        @Schema(description = "심사 처리 유형 코드", example = "APPROVE")
        String actionTypeCode,

        /** 변경 전 KYC 상태 코드 */
        @Schema(description = "변경 전 KYC 상태 코드", example = "MANUAL_REVIEW")
        String beforeStatusCode,

        /** 변경 후 KYC 상태 코드 */
        @Schema(description = "변경 후 KYC 상태 코드", example = "APPROVED")
        String afterStatusCode,

        /** 사유 코드 */
        @Schema(description = "사유 코드", example = "LOW_AI_CONFIDENCE")
        String reasonCode,

        /** 사유 */
        @Schema(description = "사유", example = "AI 신뢰도 기준 미달")
        String reason,

        /** 처리 의견 */
        @Schema(description = "처리 의견", example = "관리자 검토 후 승인")
        String comment,

        /** 심사자 유형 코드 */
        @Schema(description = "심사자 유형 코드", example = "ADMIN")
        String reviewerTypeCode,

        /** 심사자 ID */
        @Schema(description = "심사자 ID", example = "1")
        Long reviewerId,

        /** 이력 생성 시각 */
        @Schema(description = "이력 생성 시각")
        LocalDateTime createdAt
) {
}
