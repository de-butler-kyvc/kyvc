package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 모바일 로그인 응답
 *
 * @param userId 사용자 ID
 * @param userType 사용자 유형
 * @param email 사용자 이메일
 * @param name 사용자 표시명
 * @param deviceId 모바일 기기 ID
 * @param deviceRegistered 기기 등록 여부
 * @param accessTokenExpiresAt Access Token 만료 일시
 */
@Schema(description = "모바일 로그인 응답")
public record MobileLoginResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "사용자 유형", example = "CORPORATE_USER")
        String userType, // 사용자 유형
        @Schema(description = "사용자 이메일", example = "user@kyvc.local")
        String email, // 사용자 이메일
        @Schema(description = "사용자 표시명", example = "케이와이브이씨 주식회사")
        String name, // 사용자 표시명
        @Schema(description = "모바일 기기 ID", example = "device-001")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "기기 등록 여부", example = "true")
        Boolean deviceRegistered, // 기기 등록 여부
        @Schema(description = "Access Token 만료 일시", example = "2026-05-05T10:30:00")
        LocalDateTime accessTokenExpiresAt // Access Token 만료 일시
) {
}
