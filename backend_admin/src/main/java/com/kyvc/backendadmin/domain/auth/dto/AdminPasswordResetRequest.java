package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 관리자 비밀번호 재설정 요청 DTO
/**
 * 관리자 비밀번호 재설정 요청 DTO입니다.
 *
 * <p>비밀번호 재설정 대상 관리자 이메일을 전달합니다.</p>
 */
@Schema(description = "관리자 비밀번호 재설정 요청")
public record AdminPasswordResetRequest(
        @Schema(description = "비밀번호 재설정 대상 관리자 이메일", example = "admin@kyvc.com")
        @Email @NotBlank String email // 비밀번호 재설정 대상 관리자 이메일
) {
}
