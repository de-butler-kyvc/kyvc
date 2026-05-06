package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 재설정 확정 응답
 *
 * @param changed 비밀번호 변경 여부
 */
@Schema(description = "비밀번호 재설정 확정 응답")
public record PasswordResetConfirmResponse(
        @Schema(description = "비밀번호 변경 여부", example = "true")
        Boolean changed // 비밀번호 변경 여부
) {
}
