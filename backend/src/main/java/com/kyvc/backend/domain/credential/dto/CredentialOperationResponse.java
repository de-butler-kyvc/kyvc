package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VC 업무 요청 처리 응답
 *
 * @param credentialRequestId Credential 요청 ID
 * @param credentialId Credential ID
 * @param status 요청 상태
 * @param credentialStatus Credential 상태
 * @param txStatus 트랜잭션 상태
 * @param failureReason 실패 사유
 */
@Schema(description = "VC 업무 요청 처리 응답")
public record CredentialOperationResponse(
        @Schema(description = "Credential 요청 ID", example = "1")
        Long credentialRequestId, // Credential 요청 ID
        @Schema(description = "Credential ID", example = "10")
        Long credentialId, // Credential ID
        @Schema(description = "요청 상태", example = "COMPLETED")
        String status, // 요청 상태
        @Schema(description = "Credential 상태", example = "VALID")
        String credentialStatus, // Credential 상태
        @Schema(description = "트랜잭션 상태", example = "CONFIRMED")
        String txStatus, // 트랜잭션 상태
        @Schema(description = "실패 사유", example = "CORE_API_CALL_FAILED")
        String failureReason // 실패 사유
) {
}
