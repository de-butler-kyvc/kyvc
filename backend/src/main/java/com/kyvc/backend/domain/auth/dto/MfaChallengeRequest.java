package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * MFA challenge 생성 요청
 *
 * @param channel MFA 채널
 * @param purpose MFA 목적 코드
 */
@Schema(description = "MFA challenge 생성 요청")
public record MfaChallengeRequest(
        @Schema(description = "MFA 채널", example = "EMAIL")
        @NotBlank(message = "MFA 채널은 필수입니다.")
        String channel, // MFA 채널
        @Schema(description = "MFA 목적 코드", example = "LOGIN")
        @NotBlank(message = "MFA 목적은 필수입니다.")
        String purpose // MFA 목적 코드
) {
}
