package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * MFA 검증 요청
 *
 * @param challengeId MFA challenge ID
 * @param verificationCode 인증번호
 */
@Schema(description = "MFA 검증 요청")
public record MfaVerifyRequest(
        @Schema(description = "MFA challenge ID", example = "1")
        @NotBlank(message = "challengeId는 필수입니다.")
        String challengeId, // MFA challenge ID
        @Schema(description = "인증번호", example = "123456")
        @NotBlank(message = "인증번호는 필수입니다.")
        String verificationCode // 인증번호
) {
}
