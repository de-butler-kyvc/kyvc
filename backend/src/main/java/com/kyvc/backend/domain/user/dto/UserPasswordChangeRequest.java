package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새 비밀번호
 * @param newPasswordConfirm 새 비밀번호 확인값
 */
@Schema(description = "비밀번호 변경 요청")
public record UserPasswordChangeRequest(
        @Schema(description = "현재 비밀번호", example = "current-password")
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword, // 현재 비밀번호
        @Schema(description = "새 비밀번호", example = "new-password")
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword, // 새 비밀번호
        @Schema(description = "새 비밀번호 확인값", example = "new-password")
        @NotBlank(message = "새 비밀번호 확인값은 필수입니다.")
        String newPasswordConfirm // 새 비밀번호 확인값
) {
}
