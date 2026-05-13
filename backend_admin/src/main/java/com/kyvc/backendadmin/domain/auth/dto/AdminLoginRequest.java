package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 관리자 로그인 요청 DTO
/**
 * 관리자 로그인 요청 DTO입니다.
 *
 * <p>관리자 이메일과 비밀번호를 받아 로그인 인증에 사용합니다.</p>
 */
@Schema(description = "관리자 로그인 요청")
public record AdminLoginRequest(
        @Schema(description = "관리자 이메일", example = "admin@kyvc.com")
        @Email @NotBlank String email, // 관리자 이메일
        @Schema(description = "비밀번호", example = "Password123!")
        @NotBlank String password // 비밀번호
) {
}
