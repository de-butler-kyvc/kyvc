package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 MFA 인증번호 검증 요청 DTO입니다.
 */
@Schema(description = "관리자 MFA 인증번호 검증 요청")
public record AdminMfaVerifyRequest(
        /** MFA challenge 식별자입니다. */
        @Schema(description = "MFA challenge 식별자입니다.", example = "1")
        @NotBlank String challengeId,

        /** 이메일로 발송된 MFA 인증번호입니다. */
        @Schema(description = "이메일로 발송된 MFA 인증번호입니다.", example = "123456")
        @NotBlank String verificationCode
) {
}
