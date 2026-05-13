package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 모바일 기기 등록 응답
 *
 * @param deviceBindingId 기기 바인딩 ID
 * @param deviceId 모바일 기기 ID
 * @param deviceName 모바일 기기명
 * @param os 모바일 운영체제 정보
 * @param appVersion 앱 버전
 * @param deviceBindingStatus 기기 바인딩 상태
 * @param registeredAt 등록 일시
 * @param lastUsedAt 마지막 사용 일시
 */
@Schema(description = "모바일 기기 등록 응답")
public record MobileDeviceRegisterResponse(
        @Schema(description = "기기 바인딩 ID", example = "1")
        Long deviceBindingId, // 기기 바인딩 ID
        @Schema(description = "모바일 기기 ID", example = "device-001")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "모바일 기기명", example = "Galaxy S25")
        String deviceName, // 모바일 기기명
        @Schema(description = "모바일 운영체제 정보", example = "Android")
        String os, // 모바일 운영체제 정보
        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion, // 앱 버전
        @Schema(description = "기기 바인딩 상태", example = "ACTIVE")
        String deviceBindingStatus, // 기기 바인딩 상태
        @Schema(description = "등록 일시", example = "2026-05-05T10:30:00")
        LocalDateTime registeredAt, // 등록 일시
        @Schema(description = "마지막 사용 일시", example = "2026-05-05T10:30:00")
        LocalDateTime lastUsedAt // 마지막 사용 일시
) {
}
