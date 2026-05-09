package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 금융사 VP 요청 목록 응답
 *
 * @param items VP 요청 목록
 * @param page 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "금융사 VP 요청 목록 응답")
public record FinanceVpRequestListResponse(
        @Schema(description = "VP 요청 목록")
        List<FinanceVpRequestSummaryResponse> items, // VP 요청 목록
        @Schema(description = "페이지 번호", example = "0")
        int page, // 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 건수", example = "1")
        long totalElements, // 전체 건수
        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages // 전체 페이지 수
) {
}
