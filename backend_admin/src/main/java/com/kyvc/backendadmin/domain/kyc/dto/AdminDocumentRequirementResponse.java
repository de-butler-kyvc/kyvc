package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 법인 유형별 필수서류 정책 단건 응답 DTO입니다.
 *
 * <p>등록된 필수서류 정책의 식별자와 정책 속성, 생성/수정 정보를 전달합니다.</p>
 */
@Schema(description = "필수서류 정책 단건 응답")
public record AdminDocumentRequirementResponse(
        @Schema(description = "필수서류 정책 ID", example = "1")
        Long requirementId,
        @Schema(description = "법인 유형 공통코드", example = "CORPORATION")
        String corporateType,
        @Schema(description = "문서 유형 공통코드", example = "BUSINESS_REGISTRATION")
        String documentType,
        @Schema(description = "필수 여부", example = "Y")
        String requiredYn,
        @Schema(description = "사용 여부", example = "Y")
        String enabledYn,
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder,
        @Schema(description = "제출 안내 문구")
        String guideMessage,
        @Schema(description = "생성 관리자 ID", example = "1")
        Long createdByAdminId,
        @Schema(description = "수정 관리자 ID", example = "1")
        Long updatedByAdminId,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
