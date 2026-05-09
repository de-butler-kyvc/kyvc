package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core AI 심사 상태 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param checkedAt 조회 시각
 */
@Schema(description = "Core AI 심사 상태 응답")
public record CoreAiReviewStatusResponse(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "QUEUED")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "AI review status is stored in backend DB.")
        String message, // 처리 메시지
        @Schema(description = "조회 시각", example = "2026-05-06T10:05:00")
        LocalDateTime checkedAt // 조회 시각
) {
}
