package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 요청 이력 요약 응답
 *
 * @param credentialRequestId Credential 요청 ID
 * @param credentialId Credential ID
 * @param type 요청 유형
 * @param status 요청 상태
 * @param credentialStatus Credential 상태
 * @param requestedAt 요청 일시
 * @param completedAt 완료 일시
 */
@Schema(description = "Credential 요청 이력 요약 응답")
public record CredentialRequestSummaryResponse(
        @Schema(description = "Credential 요청 ID", example = "1")
        Long credentialRequestId, // Credential 요청 ID
        @Schema(description = "Credential ID", example = "10")
        Long credentialId, // Credential ID
        @Schema(description = "요청 유형", example = "REVOKE")
        String type, // 요청 유형
        @Schema(description = "요청 상태", example = "COMPLETED")
        String status, // 요청 상태
        @Schema(description = "Credential 상태", example = "REVOKED")
        String credentialStatus, // Credential 상태
        @Schema(description = "요청 일시", example = "2026-05-10T10:00:00")
        LocalDateTime requestedAt, // 요청 일시
        @Schema(description = "완료 일시", example = "2026-05-10T10:00:10")
        LocalDateTime completedAt // 완료 일시
) {
}
