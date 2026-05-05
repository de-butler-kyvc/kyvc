package com.kyvc.backendadmin.global.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 공통코드 등록 요청값을 전달하는 DTO입니다.
 */
@Schema(description = "공통코드 등록 요청")
public record CommonCodeCreateRequest(
        @Schema(description = "공통코드 그룹 ID", example = "1")
        @NotNull
        Long codeGroupId,
        @Schema(description = "코드", example = "NEW_REASON")
        @NotBlank
        String code,
        @Schema(description = "코드명", example = "신규 사유")
        @NotBlank
        String codeName,
        @Schema(description = "코드 설명")
        String description,
        @Schema(description = "정렬 순서", example = "10")
        Integer sortOrder,
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        String enabledYn,
        @Schema(description = "확장 메타데이터 JSON", example = "{}")
        String metadataJson
) {
}
