package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core 헬스 체크 응답
 *
 * @param coreMode Core 연동 모드
 * @param available Core 가용 여부
 * @param message Core 상태 메시지
 */
@Schema(description = "Core 헬스 체크 응답")
public record CoreHealthResponse(
        @Schema(description = "Core 연동 모드", example = "HTTP")
        String coreMode, // Core 연동 모드
        @Schema(description = "Core 가용 여부", example = "true")
        boolean available, // Core 가용 여부
        @Schema(description = "Core 상태 메시지", example = "kyvc-core:ok (dev)")
        String message // Core 상태 메시지
) {
}
