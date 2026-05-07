package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법인 유형별 필수서류 정책 목록 응답 DTO입니다.
 *
 * <p>검색된 필수서류 정책 목록과 기존 프로젝트의 페이지 응답 형식에 맞춘
 * 페이지 정보를 전달합니다.</p>
 */
@Schema(description = "필수서류 정책 목록 응답")
public record AdminDocumentRequirementListResponse(
        @Schema(description = "필수서류 정책 목록")
        List<Item> items,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "120")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages
) {

    /**
     * 필수서류 정책 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "필수서류 정책 목록 항목")
    public record Item(
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
}
