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

        /** AI 심사 상태 코드 */
        @Schema(description = "AI 심사 상태 코드", example = "SUCCESS")
        String aiReviewStatusCode,

        /** AI 심사 결과 코드 */
        @Schema(description = "AI 심사 결과 코드", example = "PASS")
        String aiReviewResultCode,

        /** AI 신뢰도 점수 */
        @Schema(description = "AI 신뢰도 점수", example = "92.50")
        BigDecimal aiConfidenceScore,

        /** AI 심사 요약 */
        @Schema(description = "AI 심사 요약", example = "법인명과 사업자등록번호가 제출 문서와 일치합니다.")
        String aiReviewSummary,

        /** 민감정보가 마스킹된 AI 심사 상세 JSON */
        @Schema(description = "민감정보가 마스킹된 AI 심사 상세 JSON")
        String aiReviewDetailJson,

        /** 수동심사 사유 */
        @Schema(description = "수동심사 사유", example = "관리자 확인이 필요한 신청입니다.")
        String manualReviewReason,

        /** AI 심사 사유 코드 */
        @Schema(description = "AI 심사 사유 코드", example = "LOW_AI_CONFIDENCE")
        String aiReviewReasonCode,

        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
