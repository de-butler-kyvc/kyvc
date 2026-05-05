package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 계정 목록 응답 DTO
/**
 * 관리자 계정 목록 응답 DTO입니다.
 *
 * <p>검색된 관리자 목록과 페이징 정보를 클라이언트에 전달합니다.</p>
 */
@Schema(description = "관리자 계정 목록 응답")
public record AdminUserSummaryResponse(
        @Schema(description = "관리자 목록")
        List<Item> items, // 관리자 목록
        @Schema(description = "현재 페이지 번호", example = "0")
        int page, // 현재 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 건수", example = "120")
        long totalElements, // 전체 건수
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages // 전체 페이지 수
) {
    // 관리자 목록 item DTO
    /**
     * 관리자 계정 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "관리자 목록 item")
    public record Item(
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
            @Schema(description = "생성 시각")
            LocalDateTime createdAt // 생성 시각
    ) {
    }
}
