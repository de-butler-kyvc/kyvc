package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 관리자 로그인 및 토큰 재발급 응답 DTO
/**
 * 관리자 로그인 및 토큰 재발급 응답 DTO입니다.
 *
 * <p>발급된 Access Token, Refresh Token, 만료 정보, 관리자 요약 정보와
 * 권한 목록을 클라이언트에 전달합니다.</p>
 */
@Schema(description = "관리자 로그인 및 토큰 재발급 응답")
public record AdminLoginResponse(
        @Schema(description = "Access Token 원문")
        String accessToken, // Access Token 원문
        @Schema(description = "Refresh Token 원문")
        String refreshToken, // Refresh Token 원문
        @Schema(description = "Access Token 만료까지 남은 초", example = "1800")
        long expiresIn, // Access Token 만료까지 남은 초
        @Schema(description = "로그인한 관리자 정보")
        AdminSummary admin, // 로그인한 관리자 정보
        @Schema(description = "ROLE_ 접두사가 포함된 권한 목록", example = "[\"ROLE_BACKEND_ADMIN\"]")
        List<String> roles // ROLE_ 접두사가 포함된 권한 목록
) {
    // 응답에 포함되는 관리자 요약 정보
    /**
     * 인증 응답에 포함되는 관리자 요약 DTO입니다.
     */
    @Schema(description = "관리자 요약 정보")
    public record AdminSummary(
            @Schema(description = "관리자 ID", example = "1")
            Long adminId, // 관리자 ID
            @Schema(description = "관리자 이메일", example = "admin@kyvc.com")
            String email, // 관리자 이메일
            @Schema(description = "관리자 표시 이름", example = "Backend Admin")
            String displayName, // 관리자 표시 이름
            @Schema(description = "관리자 상태", example = "ACTIVE")
            String status // 관리자 상태
    ) {
    }
}
