package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 웹 VP 로그인 QR 해석 응답
 *
 * @param type 요청 유형
 * @param requestId VP 로그인 요청 ID
 * @param nonce Core nonce
 * @param challenge Core challenge
 * @param aud Core aud
 * @param domain Core domain
 * @param presentationDefinition Presentation Definition
 * @param requiredDisclosures 필수 disclosure 경로 목록
 * @param expiresAt 만료 일시
 */
@Schema(description = "웹 VP 로그인 QR 해석 응답")
public record WebVpLoginResolveResponse(
        @Schema(description = "요청 유형", example = "VP_LOGIN_REQUEST")
        String type, // 요청 유형
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "Core nonce", example = "core-issued-nonce")
        String nonce, // Core nonce
        @Schema(description = "Core challenge", example = "core-issued-nonce")
        String challenge, // Core challenge
        @Schema(description = "Core aud", example = "kyvc-corporate-web-login")
        String aud, // Core aud
        @Schema(description = "Core domain", example = "kyvc-corporate-web-login")
        String domain, // Core domain
        @Schema(description = "Presentation Definition")
        Map<String, Object> presentationDefinition, // Presentation Definition
        @Schema(description = "필수 disclosure 경로 목록")
        List<String> requiredDisclosures, // 필수 disclosure 경로 목록
        @Schema(description = "만료 일시", example = "2026-05-13T15:30:00")
        LocalDateTime expiresAt // 만료 일시
) {

    public WebVpLoginResolveResponse {
        requiredDisclosures = requiredDisclosures == null ? List.of() : List.copyOf(requiredDisclosures);
        presentationDefinition = presentationDefinition == null ? Map.of() : presentationDefinition;
    }
}
