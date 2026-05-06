package com.kyvc.backend.domain.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공통코드 그룹 응답
 *
 * @param codeGroupId 공통코드 그룹 ID
 * @param codeGroup 공통코드 그룹 코드
 * @param codeGroupName 공통코드 그룹 표시명
 * @param description 공통코드 그룹 설명
 * @param sortOrder 정렬 순서
 * @param enabledYn 사용 여부
 * @param systemYn 시스템 필수 여부
 */
@Schema(description = "공통코드 그룹 응답")
public record CommonCodeGroupResponse(
        @Schema(description = "공통코드 그룹 ID", example = "1")
        Long codeGroupId, // 공통코드 그룹 ID
        @Schema(description = "공통코드 그룹 코드", example = "DOCUMENT_TYPE")
        String codeGroup, // 공통코드 그룹 코드
        @Schema(description = "공통코드 그룹 표시명", example = "문서 유형")
        String codeGroupName, // 공통코드 그룹 표시명
        @Schema(description = "공통코드 그룹 설명", example = "KYC 제출 문서 유형")
        String description, // 공통코드 그룹 설명
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder, // 정렬 순서
        @Schema(description = "사용 여부", example = "Y")
        String enabledYn, // 사용 여부
        @Schema(description = "시스템 필수 여부", example = "Y")
        String systemYn // 시스템 필수 여부
) {
}
