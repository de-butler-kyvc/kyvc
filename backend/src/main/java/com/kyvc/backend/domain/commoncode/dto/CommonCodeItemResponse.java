package com.kyvc.backend.domain.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공통코드 항목 응답
 *
 * @param codeId 공통코드 ID
 * @param code 공통코드 값
 * @param codeName 공통코드 표시명
 * @param description 공통코드 설명
 * @param sortOrder 정렬 순서
 * @param enabledYn 사용 여부
 * @param systemYn 시스템 필수 여부
 * @param metadataJson 메타데이터 JSON
 */
@Schema(description = "공통코드 항목 응답")
public record CommonCodeItemResponse(
        @Schema(description = "공통코드 ID", example = "1")
        Long codeId, // 공통코드 ID
        @Schema(description = "공통코드 값", example = "BUSINESS_REGISTRATION")
        String code, // 공통코드 값
        @Schema(description = "공통코드 표시명", example = "사업자등록증")
        String codeName, // 공통코드 표시명
        @Schema(description = "공통코드 설명", example = "사업자등록증 제출 문서")
        String description, // 공통코드 설명
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder, // 정렬 순서
        @Schema(description = "사용 여부", example = "Y")
        String enabledYn, // 사용 여부
        @Schema(description = "시스템 필수 여부", example = "Y")
        String systemYn, // 시스템 필수 여부
        @Schema(description = "메타데이터 JSON", example = "{}")
        String metadataJson // 메타데이터 JSON
) {
}
