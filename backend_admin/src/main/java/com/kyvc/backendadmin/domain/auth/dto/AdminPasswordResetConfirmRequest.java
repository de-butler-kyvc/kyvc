package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 관리자 비밀번호 재설정 확정 요청 DTO
/**
 * 관리자 비밀번호 재설정 확정 요청 DTO입니다.
 *
 * <p>비밀번호 재설정 토큰과 새 비밀번호를 받아 비밀번호 재설정 확정에 사용합니다.</p>
 */
@Schema(description = "관리자 비밀번호 재설정 확정 요청")
public record AdminPasswordResetConfirmRequest(
        @Schema(description = "비밀번호 재설정 토큰 원문")
        @NotBlank String resetToken, // 비밀번호 재설정 토큰 원문
        @Schema(description = "새 비밀번호", example = "NewPassword123!")
        @NotBlank String newPassword // 새 비밀번호
) {
}
