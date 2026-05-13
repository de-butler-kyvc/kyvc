package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 관리자 MFA 인증번호 검증 응답 DTO입니다.
 */
@Schema(description = "관리자 MFA 인증번호 검증 응답")
public record AdminMfaVerifyResponse(
        /** 중요 작업 승인에 사용할 MFA_SESSION 원문 토큰입니다. */
        @Schema(description = "중요 작업 승인에 사용할 MFA_SESSION 원문 토큰입니다.", example = "mfa_session_token")
        String mfaToken,

        /** MFA_SESSION 토큰 만료 시각입니다. */
        @Schema(description = "MFA_SESSION 토큰 만료 시각입니다.", example = "2026-05-06T23:59:00")
        LocalDateTime expiresAt
) {
}
