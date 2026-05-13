package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Verifier 테스트 VP 검증 요청
 *
 * @param vpJwt 검증 대상 VP JWT
 * @param format Presentation format
 * @param presentation Presentation 원문 또는 객체
 * @param nonce 테스트 nonce
 * @param requestedClaims 요청 Claim 목록
 * @param testMode 테스트 모드 여부
 */
@Schema(description = "Verifier 테스트 VP 검증 요청")
public record VerifierTestVpVerificationRequest(
        @Schema(description = "검증 대상 VP JWT", example = "vp.jwt.value")
        String vpJwt, // 검증 대상 VP JWT
        @Schema(description = "Presentation format", example = "vp+jwt")
        String format, // Presentation format
        @Schema(description = "Presentation 원문 또는 객체")
        Object presentation, // Presentation 원문 또는 객체
        @Schema(description = "테스트 nonce", example = "test-nonce")
        String nonce, // 테스트 nonce
        @Schema(description = "요청 Claim 목록")
        List<String> requestedClaims, // 요청 Claim 목록
        @Schema(description = "테스트 모드 여부", example = "true")
        Boolean testMode // 테스트 모드 여부
) {
    public VerifierTestVpVerificationRequest(
            String vpJwt, // 검증 대상 VP JWT
            String nonce, // 테스트 nonce
            List<String> requestedClaims, // 요청 Claim 목록
            Boolean testMode // 테스트 모드 여부
    ) {
        this(vpJwt, null, null, nonce, requestedClaims, testMode);
    }
}
