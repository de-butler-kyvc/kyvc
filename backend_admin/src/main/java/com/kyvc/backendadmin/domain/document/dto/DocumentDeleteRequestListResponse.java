package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 원본서류 삭제 요청 목록 응답입니다.
 *
 * @param items 삭제 요청 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "원본서류 삭제 요청 목록 응답")
public record DocumentDeleteRequestListResponse(
        /** 삭제 요청 목록 */
        @Schema(description = "삭제 요청 목록")
        List<DocumentDeleteRequestSummaryResponse> items,

        /** 현재 페이지 번호 */
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** 전체 건수 */
        @Schema(description = "전체 건수", example = "100")
        long totalElements,

        /** 전체 페이지 수 */
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
}
