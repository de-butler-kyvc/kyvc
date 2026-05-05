package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 관리자 MFA challenge 생성 응답 DTO
/**
 * 관리자 MFA challenge 생성 응답 DTO입니다.
 *
 * <p>challenge 식별자, 인증번호 만료 시각, 마스킹된 발송 대상을 전달합니다.</p>
 */
@Schema(description = "관리자 MFA challenge 생성 응답")
public record AdminMfaChallengeResponse(
        @Schema(description = "MFA challenge 식별자", example = "9a8d2e2f-0f53-46bb-8896-d9e8e2efbb9a")
        String challengeId, // MFA challenge 식별자
        @Schema(description = "인증번호 만료 시각")
        LocalDateTime expiresAt, // 인증번호 만료 시각
        @Schema(description = "마스킹된 발송 대상", example = "a***@kyvc.com")
        String maskedTarget // 마스킹된 발송 대상
) {
}
