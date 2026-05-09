package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Credential 상태 이력 응답")
public record AdminCredentialStatusHistoryResponse(
        @Schema(description = "상태 이력 ID", example = "1")
        Long historyId,
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,
        @Schema(description = "변경 전 상태 코드", example = "ISSUING")
        String beforeStatusCode,
        @Schema(description = "변경 후 상태 코드", example = "VALID")
        String afterStatusCode,
        @Schema(description = "변경 주체 유형 코드", example = "SYSTEM")
        String changedByTypeCode,
        @Schema(description = "변경 주체 ID", example = "1")
        Long changedById,
        @Schema(description = "사유 코드", example = "CORE_ISSUE_COMPLETED")
        String reasonCode,
        @Schema(description = "사유", example = "Core 발급 완료")
        String reason,
        @Schema(description = "변경 일시")
        LocalDateTime changedAt
) {
}
