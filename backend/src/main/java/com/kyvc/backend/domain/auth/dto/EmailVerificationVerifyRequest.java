package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 회원가입 이메일 인증번호 검증 요청
 *
 * @param verificationId 이메일 인증 ID
 * @param email 인증 대상 이메일
 * @param verificationCode 인증번호
 */
@Schema(description = "회원가입 이메일 인증번호 검증 요청")
public record EmailVerificationVerifyRequest(
        @Schema(description = "이메일 인증 ID", example = "1")
        @NotNull(message = "verificationId는 필수입니다.")
        Long verificationId, // 이메일 인증 ID
        @Schema(description = "인증 대상 이메일", example = "user@kyvc.local")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email, // 인증 대상 이메일
        @Schema(description = "인증번호", example = "123456")
        @NotBlank(message = "인증번호는 필수입니다.")
        String verificationCode // 인증번호
) {
}
