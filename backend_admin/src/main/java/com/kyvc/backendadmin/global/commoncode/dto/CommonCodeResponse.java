package com.kyvc.backendadmin.global.commoncode.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 공통코드 상세 정보를 전달하는 DTO입니다.
 */
@Schema(description = "공통코드 응답")
public record CommonCodeResponse(
        @Schema(description = "공통코드 ID", example = "1")
        Long codeId,
        @Schema(description = "공통코드 그룹 ID", example = "1")
        Long codeGroupId,
        @Schema(description = "공통코드 그룹", example = "DOCUMENT_TYPE")
        String codeGroup,
        @Schema(description = "공통코드 그룹명", example = "문서 유형")
        String codeGroupName,
        @Schema(description = "코드", example = "BUSINESS_REGISTRATION")
        String code,
        @Schema(description = "코드명", example = "사업자등록증")
        String codeName,
        @Schema(description = "코드 설명")
        String description,
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder,
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        String enabledYn,
        @Schema(description = "시스템 보호 여부", example = "N", allowableValues = {"Y", "N"})
        String systemYn,
        @Schema(description = "확장 메타데이터 JSON", example = "{}")
        String metadataJson,
        @Schema(description = "생성 관리자 ID", example = "1")
        Long createdByAdminId,
        @Schema(description = "수정 관리자 ID", example = "1")
        Long updatedByAdminId,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
}
