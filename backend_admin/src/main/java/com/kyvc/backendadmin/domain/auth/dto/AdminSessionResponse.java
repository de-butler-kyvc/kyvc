package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 관리자 세션 및 내 정보 조회 응답 DTO
/**
 * 관리자 세션 및 내 정보 조회 응답 DTO입니다.
 *
 * <p>인증 여부, 현재 관리자 정보, 권한 목록과 읽지 않은 알림 수를 표현합니다.</p>
 */
@Schema(description = "관리자 세션 및 내 정보 조회 응답")
public record AdminSessionResponse(
        @Schema(description = "인증 여부", example = "true")
        boolean authenticated, // 인증 여부
        @Schema(description = "현재 관리자 정보")
        AdminLoginResponse.AdminSummary admin, // 현재 관리자 정보
        @Schema(description = "ROLE_ 접두사가 포함된 권한 목록", example = "[\"ROLE_BACKEND_ADMIN\"]")
        List<String> roles, // ROLE_ 접두사가 포함된 권한 목록
        @Schema(description = "읽지 않은 알림 수", example = "0")
        long unreadNotificationCount // 읽지 않은 알림 수
) {
    // 토큰이 없는 공개 세션 조회의 비인증 응답 생성
    /**
     * 토큰이 없는 세션 조회에서 사용할 비인증 응답을 생성합니다.
     *
     * @return authenticated=false인 세션 응답
     */
    public static AdminSessionResponse unauthenticated() {
        return new AdminSessionResponse(false, null, List.of(), 0);
    }
}
