package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원가입 이메일 인증번호 검증 응답
 *
 * @param verified 인증 성공 여부
 * @param email 정규화 이메일
 */
@Schema(description = "회원가입 이메일 인증번호 검증 응답")
public record EmailVerificationVerifyResponse(
        @Schema(description = "인증 성공 여부", example = "true")
        Boolean verified, // 인증 성공 여부
        @Schema(description = "정규화 이메일", example = "user@kyvc.local")
        String email // 정규화 이메일
) {
}
