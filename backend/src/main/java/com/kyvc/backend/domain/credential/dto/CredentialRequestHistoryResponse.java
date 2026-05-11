package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 요청 이력 응답
 *
 * @param credentialRequestId Credential 요청 ID
 * @param credentialId Credential ID
 * @param requestTypeCode 요청 유형 코드
 * @param requestStatusCode 요청 상태 코드
 * @param requestMessage 요청 메시지
 * @param requestType 요청 유형
 * @param status 요청 상태
 * @param reason 요청 사유
 * @param requestedAt 요청 일시
 * @param completedAt 완료 일시
 */
@Schema(description = "Credential 요청 이력 응답")
public record CredentialRequestHistoryResponse(
        @Schema(description = "Credential 요청 ID", example = "1")
        Long credentialRequestId, // Credential 요청 ID
        @Schema(description = "Credential ID", example = "10")
        Long credentialId, // Credential ID
        @Schema(description = "요청 유형 코드", example = "REISSUE")
        String requestTypeCode, // 요청 유형 코드
        @Schema(description = "요청 상태 코드", example = "REQUESTED")
        String requestStatusCode, // 요청 상태 코드
        @Schema(description = "요청 유형", example = "REISSUE")
        String requestType, // 요청 유형
        @Schema(description = "요청 상태", example = "COMPLETED")
        String status, // 요청 상태
        @Schema(description = "요청 사유", example = "정보 갱신")
        String reason, // 요청 사유
        @Schema(description = "요청 메시지", example = "VC 재발급 요청")
        String requestMessage, // 요청 메시지
        @Schema(description = "요청 일시", example = "2026-05-10T10:00:00")
        LocalDateTime requestedAt, // 요청 일시
        @Schema(description = "완료 일시", example = "2026-05-10T10:00:03")
        LocalDateTime completedAt // 완료 일시
) {
    public CredentialRequestHistoryResponse(
            Long credentialRequestId, // Credential 요청 ID
            Long credentialId, // Credential ID
            String requestType, // 요청 유형
            String status, // 요청 상태
            String reason, // 요청 사유
            LocalDateTime requestedAt, // 요청 일시
            LocalDateTime completedAt // 완료 일시
    ) {
        this(credentialRequestId, credentialId, requestType, status, requestType, status, reason, null, requestedAt, completedAt);
    }
}
