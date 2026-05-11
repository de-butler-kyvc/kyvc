package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 이메일 인증번호 발송 요청
 *
 * @param email 인증 대상 이메일
 * @param purpose 인증 목적
 */
@Schema(description = "회원가입 이메일 인증번호 발송 요청")
public record EmailVerificationRequest(
        @Schema(description = "인증 대상 이메일", example = "user@kyvc.local")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email, // 인증 대상 이메일
        @Schema(description = "인증 목적", example = "SIGNUP")
        String purpose // 인증 목적
) {
}
