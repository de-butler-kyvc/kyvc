package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VP 검증 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 * @param completed 검증 완료 여부
 * @param valid VP 유효 여부
 * @param replaySuspected Replay 의심 여부
 * @param resultSummary 결과 요약
 */
@Schema(description = "Core VP 검증 요청 응답")
public record CoreVpVerificationResponse(
        @Schema(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "REQUESTED")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VP verification request accepted by core.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:20:00")
        LocalDateTime requestedAt, // 요청 시각
        @Schema(description = "검증 완료 여부", example = "true")
        Boolean completed, // 검증 완료 여부
        @Schema(description = "VP 유효 여부", example = "true")
        Boolean valid, // VP 유효 여부
        @Schema(description = "Replay 의심 여부", example = "false")
        Boolean replaySuspected, // Replay 의심 여부
        @Schema(description = "결과 요약", example = "VP 검증 성공")
        String resultSummary // 결과 요약
) {
}
