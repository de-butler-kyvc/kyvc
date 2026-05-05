package com.kyvc.backendadmin.global.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 공통코드 그룹 목록 조회 결과를 전달하는 DTO입니다.
 */
@Schema(description = "공통코드 그룹 목록 응답")
public record CommonCodeGroupListResponse(
        @Schema(description = "공통코드 그룹 목록")
        List<CommonCodeGroupResponse> items,
        @Schema(description = "페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "100")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
}
