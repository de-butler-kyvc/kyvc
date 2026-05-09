package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * VC 폐기 요청
 *
 * @param reason 폐기 사유
 * @param requestMessage 요청 메시지
 */
@Schema(description = "VC 폐기 요청")
public record CredentialRevokeRequest(
        @Schema(description = "폐기 사유", example = "사용자 요청")
        @NotBlank
        String reason, // 폐기 사유
        @Schema(description = "요청 메시지", example = "VC 폐기 요청")
        String requestMessage // 요청 메시지
) {
}
