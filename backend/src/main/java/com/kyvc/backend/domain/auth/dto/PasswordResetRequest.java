package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 요청 생성 요청
 *
 * @param email 사용자 이메일
 */
@Schema(description = "비밀번호 재설정 요청 생성 요청")
public record PasswordResetRequest(
        @Schema(description = "사용자 이메일", example = "user@kyvc.local")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email // 사용자 이메일
) {
}
