package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * VC 재발급 요청
 * Credential 재발급 요청
 *
 * @param reason 재발급 사유
 * @param requestMessage 요청 메시지
 */
@Schema(description = "VC 재발급 요청")
public record CredentialReissueRequest(
        @Schema(description = "재발급 사유", example = "정보 갱신")
        @NotBlank
        @Size(max = 500)
        String reason, // 재발급 사유
        @Schema(description = "요청 메시지", example = "VC 재발급 요청")
        @Size(max = 1000)
        String requestMessage // 요청 메시지
) {
}
