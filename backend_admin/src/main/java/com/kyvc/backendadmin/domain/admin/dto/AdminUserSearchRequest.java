package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// 관리자 계정 목록 검색 조건 DTO
/**
 * 관리자 계정 목록 검색 조건 DTO입니다.
 *
 * <p>관리자 목록 조회 API의 페이징, 키워드, 상태, 권한 코드 검색 조건을 표현합니다.</p>
 */
@Schema(description = "관리자 계정 목록 검색 조건")
public record AdminUserSearchRequest(
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page, // 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "이메일 또는 이름 검색어", example = "admin")
        String keyword, // 검색어
        @Schema(description = "관리자 상태", example = "ACTIVE")
        String status, // 관리자 상태
        @Schema(description = "권한 코드", example = "BACKEND_ADMIN")
        String roleCode // 권한 코드
) {
    // 기본값과 최소/최대값을 보정한 검색 조건 생성
    /**
     * 목록 조회 요청 파라미터의 기본값과 범위를 보정해 검색 조건을 생성합니다.
     *
     * @param page 요청 페이지 번호
     * @param size 요청 페이지 크기
     * @param keyword 이메일 또는 이름 검색어
     * @param status 관리자 상태
     * @param roleCode 권한 코드
     * @return 보정된 관리자 계정 검색 조건
     */
    public static AdminUserSearchRequest of(Integer page, Integer size, String keyword, String status, String roleCode) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AdminUserSearchRequest(resolvedPage, resolvedSize, keyword, status, roleCode);
    }
}
