package com.kyvc.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청
 *
 * @param email 로그인 이메일
 * @param password 로그인 비밀번호
 */
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email, // 로그인 이메일
        @NotBlank(message = "비밀번호는 필수입니다.")
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
