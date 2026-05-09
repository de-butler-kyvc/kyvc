package com.kyvc.backendadmin.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * VP 검증 목록 응답 DTO입니다.
 */
@Schema(description = "VP 검증 목록 응답")
public record AdminVpVerificationListResponse(

        /** VP 검증 목록 */
        @Schema(description = "VP 검증 목록")
        List<AdminVpVerificationSummaryResponse> items,

        /** 현재 페이지 번호 */
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** 전체 건수 */
        @Schema(description = "전체 건수", example = "120")
        long totalElements,

        /** 전체 페이지 수 */
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages
) {
}
