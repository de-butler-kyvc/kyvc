package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 모바일 보안 설정 응답
 *
 * @param deviceId 모바일 기기 ID
 * @param pinEnabled PIN 사용 여부
 * @param biometricEnabled 생체인증 사용 여부
 * @param updatedAt 수정 일시
 */
@Schema(description = "모바일 보안 설정 응답")
public record MobileSecuritySettingResponse(
        @Schema(description = "모바일 기기 ID", example = "device-001")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "PIN 사용 여부", example = "true")
        Boolean pinEnabled, // PIN 사용 여부
        @Schema(description = "생체인증 사용 여부", example = "false")
        Boolean biometricEnabled, // 생체인증 사용 여부
        @Schema(description = "수정 일시", example = "2026-05-05T10:30:00", nullable = true)
        LocalDateTime updatedAt // 수정 일시
) {
}
