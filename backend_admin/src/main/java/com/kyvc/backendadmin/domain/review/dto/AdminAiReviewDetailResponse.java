package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 심사 결과 상세 조회 응답 DTO입니다.
 */
@Schema(description = "AI 심사 결과 상세 조회 응답")
public record AdminAiReviewDetailResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** AI 심사 상태 */
        @Schema(description = "AI 심사 상태", example = "SUCCESS")
        String aiReviewStatus,

        /** AI 심사 결과 */
        @Schema(description = "AI 심사 결과", example = "PASS")
        String aiReviewResult,

        /** AI 신뢰도 점수 */
        @Schema(description = "AI 신뢰도 점수", example = "92.50")
        BigDecimal confidenceScore,

        /** 수동 심사 사유 */
        @Schema(description = "수동 심사 사유", example = "신뢰도 기준 미달")
        String manualReviewReason,

        /** 최근 AI Core 요청 ID */
        @Schema(description = "최근 AI Core 요청 ID", example = "ai-review-100-1")
        String coreRequestId,

        /** 최근 AI Core 요청 상태 */
        @Schema(description = "최근 AI Core 요청 상태", example = "SUCCESS")
        String coreRequestStatus,

        /** AI 심사 완료 또는 실패 이력 시각 */
        @Schema(description = "AI 심사 완료 또는 실패 이력 시각")
        LocalDateTime reviewedAt
) {
}
