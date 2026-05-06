package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core Callback 처리 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param received Callback 수신 여부
 * @param processed Callback 처리 여부
 * @param status Core 요청 상태
 * @param message 처리 메시지
 */
@Schema(description = "Core Callback 처리 응답")
public record CoreCallbackResponse(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Callback 수신 여부", example = "true")
        boolean received, // Callback 수신 여부
        @Schema(description = "Callback 처리 여부", example = "true")
        boolean processed, // Callback 처리 여부
        @Schema(description = "Core 요청 상태", example = "SUCCESS")
        String status, // Core 요청 상태
        @Schema(description = "처리 메시지", example = "Callback processed successfully")
        String message // 처리 메시지
) {
}
