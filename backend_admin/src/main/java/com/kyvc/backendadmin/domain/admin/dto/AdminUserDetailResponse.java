package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 계정 상세 응답 DTO
/**
 * 관리자 계정 상세 응답 DTO입니다.
 *
 * <p>관리자 계정 단건 조회, 생성, 수정 API의 상세 응답에 사용합니다.</p>
 */
@Schema(description = "관리자 계정 상세 응답")
public record AdminUserDetailResponse(
        @Schema(description = "관리자 ID", example = "1")
        Long adminId, // 관리자 ID
        @Schema(description = "관리자 이메일", example = "admin@kyvc.com")
        String email, // 관리자 이메일
        @Schema(description = "관리자 이름", example = "Backend Admin")
        String name, // 관리자 이름
        @Schema(description = "관리자 상태", example = "ACTIVE")
        String status, // 관리자 상태
        @Schema(description = "권한 목록", example = "[\"ROLE_BACKEND_ADMIN\"]")
        List<String> roles, // 권한 목록
        @Schema(description = "마지막 로그인 시각")
        LocalDateTime lastLoginAt, // 마지막 로그인 시각
        @Schema(description = "생성 시각")
        LocalDateTime createdAt, // 생성 시각
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt // 수정 시각
) {
}
