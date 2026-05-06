package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VC 발급 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core VC 발급 요청 응답")
public record CoreVcIssuanceResponse(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "ISSUING")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VC issuance request accepted by stub core.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:10:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
