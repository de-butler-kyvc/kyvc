package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core AI 심사 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core AI 심사 요청 응답")
public record CoreAiReviewResponse(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "QUEUED")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "AI review request accepted by stub core.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:00:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
