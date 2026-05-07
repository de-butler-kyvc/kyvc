package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VC 발급 상태 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param checkedAt 조회 시각
 */
@Schema(description = "Core VC 발급 상태 응답")
public record CoreVcIssuanceStatusResponse(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "ISSUING")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VC issuance is in progress in stub core.")
        String message, // 처리 메시지
        @Schema(description = "조회 시각", example = "2026-05-06T10:11:00")
        LocalDateTime checkedAt // 조회 시각
) {
}
