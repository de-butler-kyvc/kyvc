package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 발급 응답
 *
 * @param credentialId Credential ID
 * @param status Credential 상태
 * @param txStatus 트랜잭션 상태
 * @param issuedAt 발급 일시
 * @param failureReason 실패 사유 코드
 */
@Schema(description = "Credential 발급 응답")
public record CredentialIssueResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 상태", example = "VALID")
        String status, // Credential 상태
        @Schema(description = "트랜잭션 상태", example = "CONFIRMED")
        String txStatus, // 트랜잭션 상태
        @Schema(description = "발급 일시", example = "2026-05-10T10:00:00")
        LocalDateTime issuedAt, // 발급 일시
        @Schema(description = "실패 사유 코드", example = "CORE_API_TIMEOUT")
        String failureReason // 실패 사유 코드
) {
}
