package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 재설정 요청 생성 응답
 *
 * @param requested 요청 접수 여부
 */
@Schema(description = "비밀번호 재설정 요청 생성 응답")
public record PasswordResetRequestResponse(
        @Schema(description = "요청 접수 여부", example = "true")
        Boolean requested // 요청 접수 여부
) {
}
