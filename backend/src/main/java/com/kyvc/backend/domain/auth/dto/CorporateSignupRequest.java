package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 법인 사용자 회원가입 요청
 *
 * @param email 로그인 이메일
 * @param password 로그인 비밀번호
 */
@Schema(description = "법인 사용자 회원가입 요청")
public record CorporateSignupRequest(
        @Schema(description = "로그인 이메일", example = "user@kyvc.local")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email, // 로그인 이메일
        @Schema(description = "로그인 비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password // 로그인 비밀번호
) {

    /**
     * @return 로그인 이메일
     */
    public String email() {
        return email;
    }

    /**
     * @return 로그인 비밀번호
     */
    public String password() {
        return password;
    }
}
