package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Credential 요청 이력 응답")
public record AdminCredentialRequestResponse(
        @Schema(description = "Credential 요청 ID", example = "1")
        Long credentialRequestId,
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,
        @Schema(description = "요청 유형 코드", example = "ISSUE")
        String requestTypeCode,
        @Schema(description = "요청 상태 코드", example = "SUCCESS")
        String requestStatusCode,
        @Schema(description = "요청 주체 유형 코드", example = "ADMIN")
        String requestedByTypeCode,
        @Schema(description = "요청 주체 ID", example = "1")
        Long requestedById,
        @Schema(description = "사유 코드", example = "ADMIN_ISSUE")
        String reasonCode,
        @Schema(description = "사유", example = "관리자 발급 요청")
        String reason,
        @Schema(description = "Core 요청 ID", example = "VC-ISSUE-1")
        String coreRequestId,
        @Schema(description = "Core 요청 상태 코드", example = "COMPLETED")
        String coreRequestStatusCode,
        @Schema(description = "요청 일시")
        LocalDateTime requestedAt,
        @Schema(description = "완료 일시")
        LocalDateTime completedAt
) {
}
