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
 * @param userName 사용자명
 * @param phone 사용자 연락처
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
        String password, // 로그인 비밀번호
        @Schema(description = "사용자명", example = "홍길동")
        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 100, message = "사용자명은 최대 100자까지 입력할 수 있습니다.")
        String userName, // 사용자명
        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        @Size(max = 30, message = "사용자 연락처는 최대 30자까지 입력할 수 있습니다.")
        String phone // 사용자 연락처
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

    /**
     * @return 사용자명
     */
    public String userName() {
        return userName;
    }

    /**
     * @return 사용자 연락처
     */
    public String phone() {
        return phone;
    }
}
