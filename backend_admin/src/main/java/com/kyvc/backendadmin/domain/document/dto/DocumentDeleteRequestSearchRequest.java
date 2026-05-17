package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 원본서류 삭제 요청 목록 검색 조건
 */
@Schema(description = "원본서류 삭제 요청 목록 검색 조건")
public record DocumentDeleteRequestSearchRequest(
        /** 페이지 번호 */
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** 삭제 요청 상태 */
        @Schema(description = "삭제 요청 상태", example = "REQUESTED")
        String status,

        /** 검색어 */
        @Schema(description = "법인명, 파일명, 문서 유형, 이메일 검색어", example = "business")
        String keyword,

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "30")
        Long corporateId,

        /** 문서 ID */
        @Schema(description = "문서 ID", example = "20")
        Long documentId,

        /** 요청 사용자 ID */
        @Schema(description = "요청 사용자 ID", example = "40")
        Long requesterId,

        /** 삭제 요청 시작일 */
        @Schema(description = "삭제 요청 시작일", example = "2026-05-01")
        LocalDate startDate,

        /** 삭제 요청 종료일 */
        @Schema(description = "삭제 요청 종료일", example = "2026-05-31")
        LocalDate endDate
) {
    /**
     * 검색 조건 보정
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 삭제 요청 상태
     * @param keyword 검색어
     * @param kycId KYC 신청 ID
     * @param corporateId 법인 ID
     * @param documentId 문서 ID
     * @param requesterId 요청 사용자 ID
     * @param startDate 삭제 요청 시작일
     * @param endDate 삭제 요청 종료일
     * @return 보정된 검색 조건
     */
    public static DocumentDeleteRequestSearchRequest of(
            Integer page,
            Integer size,
            String status,
            String keyword,
            Long kycId,
            Long corporateId,
            Long documentId,
            Long requesterId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new DocumentDeleteRequestSearchRequest(
                page == null || page < 0 ? 0 : page,
                size == null || size < 1 ? 15 : Math.min(size, 100),
                status,
                keyword,
                kycId,
                corporateId,
                documentId,
                requesterId,
                startDate,
                endDate
        );
    }
}
