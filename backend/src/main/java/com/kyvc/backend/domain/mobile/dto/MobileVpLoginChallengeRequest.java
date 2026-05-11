package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 모바일 VP 로그인 challenge 요청
 *
 * @param deviceId 모바일 기기 ID
 * @param deviceName 모바일 기기명
 * @param os 모바일 운영체제
 * @param appVersion 앱 버전
 */
@Schema(description = "모바일 VP 로그인 challenge 요청")
public record MobileVpLoginChallengeRequest(
        @Schema(description = "모바일 기기 ID", example = "mobile-device-001")
        @NotBlank(message = "deviceId는 필수입니다.")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "모바일 기기명", example = "Galaxy S25")
        String deviceName, // 모바일 기기명
        @Schema(description = "모바일 운영체제", example = "Android")
        String os, // 모바일 운영체제
        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion // 앱 버전
) {
}
