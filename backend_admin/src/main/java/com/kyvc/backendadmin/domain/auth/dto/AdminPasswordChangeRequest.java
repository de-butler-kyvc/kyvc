package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 로그인한 관리자 비밀번호 변경 요청 DTO
/**
 * 로그인한 관리자 비밀번호 변경 요청 DTO입니다.
 *
 * <p>현재 비밀번호와 새 비밀번호를 받아 본인 비밀번호 변경에 사용합니다.</p>
 */
@Schema(description = "로그인한 관리자 비밀번호 변경 요청")
public record AdminPasswordChangeRequest(
        @Schema(description = "현재 비밀번호", example = "Password123!")
        @NotBlank String currentPassword, // 현재 비밀번호
        @Schema(description = "새 비밀번호", example = "NewPassword123!")
        @NotBlank String newPassword // 새 비밀번호
) {
}
