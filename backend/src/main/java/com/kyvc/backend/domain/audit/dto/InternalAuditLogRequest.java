package com.kyvc.backend.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 내부 감사로그 기록 요청
 *
 * @param actorType 행위자 유형 코드
 * @param actorId 행위자 ID
 * @param actionType 작업 유형
 * @param auditTargetType 감사 대상 유형 코드
 * @param targetId 대상 ID
 * @param requestSummary 요청 요약
 * @param ipAddress 요청 IP 주소
 */
@Schema(description = "내부 감사로그 기록 요청")
public record InternalAuditLogRequest(
        @Schema(description = "행위자 유형 코드", example = "SYSTEM")
        @NotBlank(message = "행위자 유형 코드는 필수입니다.")
        String actorType, // 행위자 유형 코드
        @Schema(description = "행위자 ID", example = "0")
        @NotNull(message = "행위자 ID는 필수입니다.")
        Long actorId, // 행위자 ID
        @Schema(description = "작업 유형", example = "INTERNAL_AUDIT_LOG_CREATE")
        @NotBlank(message = "작업 유형은 필수입니다.")
        String actionType, // 작업 유형
        @Schema(description = "감사 대상 유형 코드", example = "KYC_APPLICATION")
        @NotBlank(message = "감사 대상 유형 코드는 필수입니다.")
        String auditTargetType, // 감사 대상 유형 코드
        @Schema(description = "대상 ID", example = "1")
        @NotNull(message = "대상 ID는 필수입니다.")
        Long targetId, // 대상 ID
        @Schema(description = "요청 요약", example = "내부 감사로그 테스트", nullable = true)
        String requestSummary, // 요청 요약
        @Schema(description = "요청 IP 주소", example = "127.0.0.1", nullable = true)
        String ipAddress // 요청 IP 주소
) {
}
