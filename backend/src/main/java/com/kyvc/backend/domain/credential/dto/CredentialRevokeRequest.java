package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Credential 폐기 요청
 *
 * @param reason 폐기 사유
 */
@Schema(description = "Credential 폐기 요청")
public record CredentialRevokeRequest(
        @Schema(description = "폐기 사유", example = "사용자 요청 폐기")
        @Size(max = 1000)
        String reason // 폐기 사유
) {
}
