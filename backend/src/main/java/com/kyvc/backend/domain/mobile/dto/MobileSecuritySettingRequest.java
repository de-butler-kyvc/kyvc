package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 모바일 보안 설정 저장 요청
 *
 * @param deviceId 모바일 기기 ID
 * @param pinEnabled PIN 사용 여부
 * @param biometricEnabled 생체인증 사용 여부
 */
@Schema(description = "모바일 보안 설정 저장 요청")
public record MobileSecuritySettingRequest(
        @Schema(description = "모바일 기기 ID", example = "device-001")
        @NotBlank(message = "기기 ID는 필수입니다.")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "PIN 사용 여부", example = "true")
        Boolean pinEnabled, // PIN 사용 여부
        @Schema(description = "생체인증 사용 여부", example = "false")
        Boolean biometricEnabled // 생체인증 사용 여부
) {
}
