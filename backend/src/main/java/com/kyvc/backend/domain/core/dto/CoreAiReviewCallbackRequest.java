package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core AI 심사 Callback 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param status Callback 처리 상태
 * @param confidenceScore AI 신뢰도 점수
 * @param summary AI 심사 요약
 * @param detailJson AI 심사 상세 JSON
 * @param errorMessage 실패 메시지
 * @param reviewedAt 심사 완료 시각
 */
@Schema(description = "Core AI 심사 Callback 요청")
public record CoreAiReviewCallbackRequest(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Callback 처리 상태", example = "SUCCESS")
        String status, // Callback 처리 상태
        @Schema(description = "AI 신뢰도 점수", example = "98.50")
        BigDecimal confidenceScore, // AI 신뢰도 점수
        @Schema(description = "AI 심사 요약", example = "AI 심사 완료")
        String summary, // AI 심사 요약
        @Schema(description = "AI 심사 상세 JSON", example = "{\"riskLevel\":\"LOW\"}")
        String detailJson, // AI 심사 상세 JSON
        @Schema(description = "실패 메시지", example = "AI review failed")
        String errorMessage, // 실패 메시지
        @Schema(description = "심사 완료 시각", example = "2026-05-06T14:00:00")
        LocalDateTime reviewedAt // 심사 완료 시각
) {
}
