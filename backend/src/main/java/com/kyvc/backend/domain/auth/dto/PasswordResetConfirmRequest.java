package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 확정 요청
 *
 * @param resetToken 비밀번호 재설정 토큰
 * @param newPassword 새 비밀번호
 */
@Schema(description = "비밀번호 재설정 확정 요청")
public record PasswordResetConfirmRequest(
        @Schema(description = "비밀번호 재설정 토큰", example = "reset-token")
        @NotBlank(message = "resetToken은 필수입니다.")
        String resetToken, // 비밀번호 재설정 토큰
        @Schema(description = "새 비밀번호", example = "password123!")
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, message = "새 비밀번호는 최소 8자 이상이어야 합니다.")
        String newPassword // 새 비밀번호
) {
}
