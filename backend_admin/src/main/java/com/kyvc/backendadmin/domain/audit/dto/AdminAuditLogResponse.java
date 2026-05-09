package com.kyvc.backendadmin.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 감사로그 상세 정보를 전달하는 DTO입니다.
 */
@Schema(description = "감사로그 응답")
public record AdminAuditLogResponse(
        @Schema(description = "감사로그 ID", example = "1")
        Long auditId,
        @Schema(description = "행위자 유형", example = "ADMIN")
        String actorType,
        @Schema(description = "행위자 ID", example = "1")
        Long actorId,
        @Schema(description = "작업 유형", example = "COMMON_CODE_CREATE")
        String actionType,
        @Schema(description = "대상 유형", example = "COMMON_CODE")
        String targetType,
        @Schema(description = "대상 ID", example = "10")
        Long targetId,
        @Schema(description = "요청 요약")
        String requestSummary,
        @Schema(description = "변경 전 값 JSON")
        String beforeValueJson,
        @Schema(description = "변경 후 값 JSON")
        String afterValueJson,
        @Schema(description = "요청 IP")
        String ipAddress,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {
}
