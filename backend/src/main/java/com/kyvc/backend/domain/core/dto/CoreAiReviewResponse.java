package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core AI 심사 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param assessmentStatus Core 심사 상태
 * @param assessmentId Core 심사 결과 ID
 * @param confidenceScore AI 신뢰도 점수
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core AI 심사 요청 응답")
public record CoreAiReviewResponse(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "LOW_CONFIDENCE")
        String status, // 처리 상태
        @Schema(description = "Core 심사 상태", example = "MANUAL_REVIEW_REQUIRED")
        String assessmentStatus, // Core 심사 상태
        @Schema(description = "Core 심사 결과 ID", example = "assessment-001")
        String assessmentId, // Core 심사 결과 ID
        @Schema(description = "AI 신뢰도 점수", example = "0.82")
        BigDecimal confidenceScore, // AI 신뢰도 점수
        @Schema(description = "처리 메시지", example = "Core AI 심사가 완료되었습니다.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:00:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
