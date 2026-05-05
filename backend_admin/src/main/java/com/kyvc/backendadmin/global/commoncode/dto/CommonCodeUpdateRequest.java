package com.kyvc.backendadmin.global.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 공통코드 수정 요청값을 전달하는 DTO입니다.
 */
@Schema(description = "공통코드 수정 요청")
public record CommonCodeUpdateRequest(
        @Schema(description = "코드명", example = "변경된 사유")
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
