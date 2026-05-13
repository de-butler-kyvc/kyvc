package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모바일 VP 로그인 challenge 응답
 *
 * @param requestId VP 로그인 요청 ID
 * @param challenge VP challenge
 * @param nonce VP nonce
 * @param domain VP domain
 * @param aud VP aud
 * @param expiresAt 만료 일시
 * @param requiredClaims 필수 Claim 목록
 */
@Schema(description = "모바일 VP 로그인 challenge 응답")
public record MobileVpLoginChallengeResponse(
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "VP challenge", example = "challenge-value")
        String challenge, // VP challenge
        @Schema(description = "VP nonce", example = "nonce-value")
        String nonce, // VP nonce
        @Schema(description = "VP domain", example = "kyvc-mobile")
        String domain, // VP domain
        @Schema(description = "VP aud", example = "kyvc-mobile-login")
        String aud, // VP aud
        @Schema(description = "만료 일시", example = "2026-05-11T12:05:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "필수 Claim 목록")
        List<String> requiredClaims // 필수 Claim 목록
) {
}
