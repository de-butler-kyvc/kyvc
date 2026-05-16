package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 모바일 자동로그인 응답
 *
 * @param autoLogin 자동로그인 성공 여부
 * @param userId 사용자 ID
 * @param corporateId 법인 ID
 * @param email 사용자 이메일
 * @param corporateName 법인명
 * @param roleCode 역할 코드
 * @param accessTokenExpiresAt Access Token 만료 일시
 * @param refreshTokenExpiresAt Refresh Token 만료 일시
 */
@Schema(description = "모바일 자동로그인 응답")
public record MobileAutoLoginResponse(
        @Schema(description = "자동로그인 성공 여부", example = "true")
        boolean autoLogin, // 자동로그인 성공 여부
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "법인 ID", example = "10", nullable = true)
        Long corporateId, // 법인 ID
        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email, // 사용자 이메일
        @Schema(description = "법인명", example = "테스트 법인", nullable = true)
        String corporateName, // 법인명
        @Schema(description = "역할 코드", example = "CORPORATE_USER")
        String roleCode, // 역할 코드
        @Schema(description = "Access Token 만료 일시", example = "2026-05-16T22:00:00")
        LocalDateTime accessTokenExpiresAt, // Access Token 만료 일시
        @Schema(description = "Refresh Token 만료 일시", example = "2026-06-15T22:00:00")
        LocalDateTime refreshTokenExpiresAt // Refresh Token 만료 일시
) {
}
