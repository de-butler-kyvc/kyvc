package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core Credential 폐기 응답
 *
 * @param revoked 폐기 성공 여부
 * @param statusMode 상태 저장 모드
 * @param message 처리 메시지
 */
@Schema(description = "Core Credential 폐기 응답")
public record CoreRevokeCredentialResponse(
        @Schema(description = "폐기 성공 여부", example = "true")
        boolean revoked, // 폐기 성공 여부
        @Schema(description = "상태 저장 모드", example = "xrpl")
        String statusMode, // 상태 저장 모드
        @Schema(description = "처리 메시지", example = "Credential revoked.")
        String message // 처리 메시지
) {
}
