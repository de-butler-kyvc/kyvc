package com.kyvc.backend.domain.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 공통코드 배치 조회 응답
 *
 * @param groups 공통코드 그룹 응답 목록
 */
@Schema(description = "공통코드 배치 조회 응답")
public record CommonCodeBatchResponse(
        @Schema(description = "공통코드 그룹 응답 목록")
        List<CommonCodeResponse> groups // 공통코드 그룹 응답 목록
) {
}
