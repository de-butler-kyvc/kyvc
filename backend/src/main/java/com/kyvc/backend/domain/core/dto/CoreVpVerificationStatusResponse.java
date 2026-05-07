package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VP 검증 상태 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param checkedAt 조회 시각
 */
@Schema(description = "Core VP 검증 상태 응답")
public record CoreVpVerificationStatusResponse(
        @Schema(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "REQUESTED")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VP verification is requested in stub core.")
        String message, // 처리 메시지
        @Schema(description = "조회 시각", example = "2026-05-06T10:21:00")
        LocalDateTime checkedAt // 조회 시각
) {
}
