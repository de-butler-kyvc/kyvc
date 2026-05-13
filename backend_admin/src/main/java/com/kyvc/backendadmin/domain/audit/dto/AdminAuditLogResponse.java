package com.kyvc.backendadmin.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 감사로그 응답 DTO입니다.
 */
@Schema(description = "감사로그 응답")
public record AdminAuditLogResponse(
        /** 감사로그 ID */
        @Schema(description = "감사로그 ID", example = "1")
        Long auditLogId,
        /** 행위자 유형 코드 */
        @Schema(description = "행위자 유형 코드", example = "ADMIN")
        String actorTypeCode,
        /** 행위자 ID */
        @Schema(description = "행위자 ID", example = "1")
        Long actorId,
        /** 행위자 이름 */
        @Schema(description = "행위자 이름")
        String actorName,
        /** 작업 유형 */
        @Schema(description = "작업 유형", example = "VERIFIER_CREATED")
        String actionType,
        /** 대상 유형 */
        @Schema(description = "대상 유형", example = "VERIFIER")
        String targetType,
        /** 대상 ID */
        @Schema(description = "대상 ID", example = "10")
        Long targetId,
        /** 요청 요약 */
        @Schema(description = "요청 요약")
        String requestSummary,
        /** 변경 전 JSON */
        @Schema(description = "변경 전 JSON. 민감정보는 마스킹된다.")
        String beforeValueJson,
        /** 변경 후 JSON */
        @Schema(description = "변경 후 JSON. 민감정보는 마스킹된다.")
        String afterValueJson,
        /** Trace ID */
        @Schema(description = "Trace ID")
        String traceId,
        /** 요청 IP */
        @Schema(description = "요청 IP")
        String ipAddress,
        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {
}
