package com.kyvc.backendadmin.domain.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보안 이벤트/민감정보 접근 로그 DTO 모음입니다.
 */
public final class AdminSecurityDtos {
    private AdminSecurityDtos() {
    }

    @Schema(description = "보안 이벤트 응답")
    public record EventResponse(
            /** 감사로그 ID */
            @Schema(description = "감사로그 ID", example = "1")
            Long auditLogId,
            /** 이벤트 유형 */
            @Schema(description = "이벤트 유형", example = "LOGIN_FAILED")
            String eventType,
            /** 관리자 ID */
            @Schema(description = "관리자 ID", example = "1")
            Long adminId,
            /** 대상 유형 */
            @Schema(description = "대상 유형")
            String targetType,
            /** 대상 ID */
            @Schema(description = "대상 ID")
            Long targetId,
            /** 요청 요약 */
            @Schema(description = "요청 요약")
            String summary,
            /** IP */
            @Schema(description = "IP")
            String ipAddress,
            /** Trace ID */
            @Schema(description = "Trace ID")
            String traceId,
            /** 발생 시각 */
            @Schema(description = "발생 시각")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "보안 이벤트 페이지 응답")
    public record PageResponse(List<EventResponse> items, int page, int size, long totalElements, int totalPages) {
    }
}
