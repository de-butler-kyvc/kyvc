package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 웹 VP 로그인 QR 해석 요청
 *
 * @param qrToken QR 토큰 원문
 * @param deviceId 모바일 기기 ID
 */
@Schema(description = "웹 VP 로그인 QR 해석 요청")
public record WebVpLoginResolveRequest(
        @Schema(description = "QR 토큰 원문")
        String qrToken, // QR 토큰 원문
        @Schema(description = "모바일 기기 ID", example = "android-device-id")
        String deviceId // 모바일 기기 ID
) {
}
