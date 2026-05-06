package com.kyvc.backend.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 감사로그 저장 명령
 *
 * @param actorType 행위자 유형 코드
 * @param actorId 행위자 ID
 * @param actionType 작업 유형
 * @param auditTargetType 감사 대상 유형 코드
 * @param targetId 대상 ID
 * @param requestSummary 요청 요약
 * @param ipAddress 요청 IP 주소
 */
@Schema(description = "감사로그 저장 명령")
public record AuditLogCreateCommand(
        @Schema(description = "행위자 유형 코드", example = "USER")
        String actorType, // 행위자 유형 코드
        @Schema(description = "행위자 ID", example = "1")
        Long actorId, // 행위자 ID
        @Schema(description = "작업 유형", example = "NOTIFICATION_READ")
        String actionType, // 작업 유형
        @Schema(description = "감사 대상 유형 코드", example = "NOTIFICATION")
        String auditTargetType, // 감사 대상 유형 코드
        @Schema(description = "대상 ID", example = "10", nullable = true)
        Long targetId, // 대상 ID
        @Schema(description = "요청 요약", example = "알림 읽음 처리")
        String requestSummary, // 요청 요약
        @Schema(description = "요청 IP 주소", example = "127.0.0.1", nullable = true)
        String ipAddress // 요청 IP 주소
) {
}
