package com.kyvc.backend.domain.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 공통코드 그룹별 응답
 *
 * @param codeGroup 공통코드 그룹 코드
 * @param codeGroupName 공통코드 그룹 표시명
 * @param items 공통코드 항목 목록
 */
@Schema(description = "공통코드 그룹별 응답")
public record CommonCodeResponse(
        @Schema(description = "공통코드 그룹 코드", example = "DOCUMENT_TYPE")
        String codeGroup, // 공통코드 그룹 코드
        @Schema(description = "공통코드 그룹 표시명", example = "문서 유형")
        String codeGroupName, // 공통코드 그룹 표시명
        @Schema(description = "공통코드 항목 목록")
        List<CommonCodeItemResponse> items // 공통코드 항목 목록
) {
}
