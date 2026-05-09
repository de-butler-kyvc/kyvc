package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "문서 삭제 요청 검색 조건")
public record AdminDocumentDeleteRequestSearchRequest(
        @Schema(description = "요청 상태 코드", example = "REQUESTED")
        String status,
        @Schema(description = "법인명, 파일명, 요청자 이메일 검색어", example = "KYVC")
        String keyword,
        @Schema(description = "요청 시작일", example = "2026-05-01")
        LocalDate fromDate,
        @Schema(description = "요청 종료일", example = "2026-05-31")
        LocalDate toDate,
        @Schema(description = "페이지 번호", example = "0")
        Integer page,
        @Schema(description = "페이지 크기", example = "20")
        Integer size
) {
    public int normalizedPage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize() {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
