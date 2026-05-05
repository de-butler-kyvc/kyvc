package com.kyvc.backendadmin.global.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 공통코드 등록 요청값을 전달하는 DTO입니다.
 */
@Schema(description = "공통코드 등록 요청")
public record CommonCodeCreateRequest(
        @Schema(description = "공통코드 그룹 ID. codeGroup과 둘 중 하나를 전달합니다.", example = "1")
        @JsonAlias("code_group_id")
        Long codeGroupId,
        @Schema(description = "공통코드 그룹. codeGroupId와 둘 중 하나를 전달합니다.", example = "DOCUMENT_TYPE")
        @JsonAlias("code_group")
        String codeGroup,
        @Schema(description = "코드", example = "NEW_REASON")
        @NotBlank
        String code,
        @Schema(description = "코드명", example = "신규 사유")
        @JsonAlias("code_name")
        @NotBlank
        String codeName,
        @Schema(description = "코드 설명")
        String description,
        @Schema(description = "정렬 순서", example = "10")
        @JsonAlias("sort_order")
        Integer sortOrder,
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        @JsonAlias("enabled_yn")
        String enabledYn,
        @Schema(description = "확장 메타데이터 JSON", example = "{}")
        @JsonAlias("metadata_json")
        String metadataJson
) {
}
