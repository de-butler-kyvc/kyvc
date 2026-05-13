package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 웹 VP 로그인 시작 응답
 *
 * @param requestId VP 로그인 요청 ID
 * @param qrPayload QR 표시 payload
 * @param expiresAt 만료 일시
 */
@Schema(description = "웹 VP 로그인 시작 응답")
public record WebVpLoginStartResponse(
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "QR 표시 payload")
        QrPayload qrPayload, // QR 표시 payload
        @Schema(description = "만료 일시", example = "2026-05-13T15:30:00")
        LocalDateTime expiresAt // 만료 일시
) {

    /**
     * QR 표시 payload
     *
     * @param type QR 요청 유형
     * @param requestId VP 로그인 요청 ID
     * @param qrToken QR 토큰 원문
     */
    @Schema(description = "QR 표시 payload")
    public record QrPayload(
            @Schema(description = "QR 요청 유형", example = "VP_LOGIN_REQUEST")
            String type, // QR 요청 유형
            @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
            String requestId, // VP 로그인 요청 ID
            @Schema(description = "QR 토큰 원문")
            String qrToken // QR 토큰 원문
    ) {
    }
}
