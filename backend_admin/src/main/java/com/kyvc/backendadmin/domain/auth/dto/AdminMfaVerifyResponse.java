package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 관리자 MFA 인증번호 검증 응답 DTO
/**
 * 관리자 MFA 인증번호 검증 응답 DTO입니다.
 *
 * <p>MFA 검증 성공 후 발급된 MFA_SESSION 토큰 원문과 만료 시각을 전달합니다.</p>
 */
@Schema(description = "관리자 MFA 인증번호 검증 응답")
public record AdminMfaVerifyResponse(
        @Schema(description = "MFA 세션 토큰 원문")
        String mfaToken, // MFA 세션 토큰 원문
        @Schema(description = "MFA 세션 토큰 만료 시각")
        LocalDateTime expiresAt // MFA 세션 토큰 만료 시각
) {
}
