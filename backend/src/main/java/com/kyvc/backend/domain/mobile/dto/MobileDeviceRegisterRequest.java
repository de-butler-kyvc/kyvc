package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 모바일 기기 등록 요청
 *
 * @param deviceId 모바일 기기 ID
 * @param deviceName 모바일 기기명
 * @param os 모바일 운영체제 정보
 * @param appVersion 앱 버전
 * @param publicKey 기기 공개키
 */
@Schema(description = "모바일 기기 등록 요청")
public record MobileDeviceRegisterRequest(
        @Schema(description = "모바일 기기 ID", example = "device-001")
        @NotBlank(message = "기기 ID는 필수입니다.")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "모바일 기기명", example = "Galaxy S25")
        String deviceName, // 모바일 기기명
        @Schema(description = "모바일 운영체제 정보", example = "Android")
        @NotBlank(message = "운영체제 정보는 필수입니다.")
        String os, // 모바일 운영체제 정보
        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion, // 앱 버전
        @Schema(description = "기기 공개키", example = "public-key-ref")
        String publicKey // 기기 공개키
) {
}
