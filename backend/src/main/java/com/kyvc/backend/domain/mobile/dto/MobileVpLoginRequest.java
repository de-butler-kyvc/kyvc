package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 모바일 VP 로그인 요청
 *
 * @param requestId VP 로그인 요청 ID
 * @param credentialId Credential ID
 * @param vpJwt VP JWT 원문
 * @param format VP format
 * @param nonce VP nonce
 * @param challenge VP challenge
 * @param deviceId 모바일 기기 ID
 */
@Schema(description = "모바일 VP 로그인 요청")
public record MobileVpLoginRequest(
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        @NotBlank(message = "requestId는 필수입니다.")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "Credential ID", example = "1")
        @NotNull(message = "credentialId는 필수입니다.")
        Long credentialId, // Credential ID
        @Schema(description = "VP JWT 원문", example = "eyJhbGciOi...")
        @NotBlank(message = "vpJwt는 필수입니다.")
        String vpJwt, // VP JWT 원문
        @Schema(description = "VP format", example = "vp+jwt")
        String format, // VP format
        @Schema(description = "VP nonce", example = "nonce-value")
        @NotBlank(message = "nonce는 필수입니다.")
        String nonce, // VP nonce
        @Schema(description = "VP challenge", example = "challenge-value")
        @NotBlank(message = "challenge는 필수입니다.")
        String challenge, // VP challenge
        @Schema(description = "모바일 기기 ID", example = "mobile-device-001")
        @NotBlank(message = "deviceId는 필수입니다.")
        String deviceId // 모바일 기기 ID
) {
}
