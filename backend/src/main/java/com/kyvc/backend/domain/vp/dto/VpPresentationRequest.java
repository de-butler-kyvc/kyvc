package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VP 제출 요청
 *
 * @param requestId VP 요청 ID
 * @param credentialId 제출할 Credential ID
 * @param nonce VP 요청 nonce
 * @param challenge VP 요청 challenge
 * @param vpJwt VP JWT 원문
 * @param deviceId 모바일 기기 ID
 */
@Schema(description = "VP 제출 요청")
public record VpPresentationRequest(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "제출할 Credential ID", example = "1")
        Long credentialId, // 제출할 Credential ID
        @Schema(description = "VP 요청 nonce", example = "nonce-001")
        String nonce, // VP 요청 nonce
        @Schema(description = "VP 요청 challenge", example = "challenge-001")
        String challenge, // VP 요청 challenge
        @Schema(description = "VP JWT 원문", example = "vp.jwt.value")
        String vpJwt, // VP JWT 원문
        @Schema(description = "모바일 기기 ID", example = "device-001")
        String deviceId // 모바일 기기 ID
) {
    public VpPresentationRequest(
            String requestId, // VP 요청 ID
            Long credentialId, // 제출할 Credential ID
            String nonce, // VP 요청 nonce
            String challenge, // VP 요청 challenge
            String vpJwt // VP JWT 원문
    ) {
        this(requestId, credentialId, nonce, challenge, vpJwt, null);
    }
}
