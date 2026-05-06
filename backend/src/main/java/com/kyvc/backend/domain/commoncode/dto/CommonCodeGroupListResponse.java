package com.kyvc.backend.domain.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 공통코드 그룹 목록 응답
 *
 * @param items 공통코드 그룹 목록
 */
@Schema(description = "공통코드 그룹 목록 응답")
public record CommonCodeGroupListResponse(
        @Schema(description = "공통코드 그룹 목록")
        List<CommonCodeGroupResponse> items // 공통코드 그룹 목록
) {
}
