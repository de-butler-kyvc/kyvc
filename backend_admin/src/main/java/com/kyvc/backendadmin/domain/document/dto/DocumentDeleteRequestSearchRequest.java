package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 원본서류 삭제 요청 목록 검색 조건입니다.
 *
 * <p>관리자가 삭제 요청 상태, 검색어, 요청일 기간, 페이지 조건으로 목록을 조회할 때 사용합니다.</p>
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
        @Schema(description = "삭제 요청 상태", example = "REQUESTED", allowableValues = {"REQUESTED", "APPROVED", "REJECTED"})
        String status,

        /** 검색어 */
        @Schema(description = "법인명, 파일명, 문서 유형, 이메일 검색어", example = "business")
        String keyword,

        /** 삭제 요청 시작일 */
        @Schema(description = "삭제 요청 시작일", example = "2026-05-01")
        LocalDate fromDate,

        /** 삭제 요청 종료일 */
        @Schema(description = "삭제 요청 종료일", example = "2026-05-31")
        LocalDate toDate
) {
    /**
     * 검색 조건의 페이지 값을 보정합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 삭제 요청 상태
     * @param keyword 검색어
     * @param fromDate 삭제 요청 시작일
     * @param toDate 삭제 요청 종료일
     * @return 보정된 검색 조건
     */
    public static DocumentDeleteRequestSearchRequest of(
            Integer page,
            Integer size,
            String status,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return new DocumentDeleteRequestSearchRequest(
                page == null || page < 0 ? 0 : page,
                size == null || size < 1 ? 20 : Math.min(size, 100),
                status,
                keyword,
                fromDate,
                toDate
        );
    }
}
