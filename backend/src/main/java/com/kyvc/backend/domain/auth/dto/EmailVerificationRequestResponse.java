package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 회원가입 이메일 인증번호 발송 응답
 *
 * @param verificationId 이메일 인증 ID
 * @param maskedEmail 마스킹 이메일
 * @param expiresAt 인증 만료 일시
 * @param requested 요청 완료 여부
 */
@Schema(description = "회원가입 이메일 인증번호 발송 응답")
public record EmailVerificationRequestResponse(
        @Schema(description = "이메일 인증 ID", example = "1")
        Long verificationId, // 이메일 인증 ID
        @Schema(description = "마스킹 이메일", example = "u***@kyvc.local")
        String maskedEmail, // 마스킹 이메일
        @Schema(description = "인증 만료 일시", example = "2026-05-11T12:05:00")
        LocalDateTime expiresAt, // 인증 만료 일시
        @Schema(description = "요청 완료 여부", example = "true")
        Boolean requested // 요청 완료 여부
) {
}
