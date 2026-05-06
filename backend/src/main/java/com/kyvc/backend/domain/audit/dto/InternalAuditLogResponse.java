package com.kyvc.backend.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내부 감사로그 기록 응답
 *
 * @param auditId 감사로그 ID
 * @param logged 기록 여부
 */
@Schema(description = "내부 감사로그 기록 응답")
public record InternalAuditLogResponse(
        @Schema(description = "감사로그 ID", example = "100")
        Long auditId, // 감사로그 ID
        @Schema(description = "기록 여부", example = "true")
        Boolean logged // 기록 여부
) {
}
