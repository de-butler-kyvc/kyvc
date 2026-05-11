package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Verifier 사용량 통계 응답
 *
 * @param totalCount 전체 건수
 * @param successCount 성공 건수
 * @param failureCount 실패 건수
 * @param items 기간별 사용량
 */
@Schema(description = "Verifier 사용량 통계 응답")
public record VerifierUsageStatsResponse(
        @Schema(description = "전체 건수", example = "100")
        long totalCount, // 전체 건수
        @Schema(description = "성공 건수", example = "90")
        long successCount, // 성공 건수
        @Schema(description = "실패 건수", example = "10")
        long failureCount, // 실패 건수
        @Schema(description = "기간별 사용량")
        List<Item> items // 기간별 사용량
) {

    /**
     * 기간별 사용량 항목
     *
     * @param date 집계 기준일
     * @param count 전체 건수
     * @param successCount 성공 건수
     * @param failureCount 실패 건수
     */
    public record Item(
            @Schema(description = "집계 기준일", example = "2026-05-11")
            String date, // 집계 기준일
            @Schema(description = "전체 건수", example = "10")
            long count, // 전체 건수
            @Schema(description = "성공 건수", example = "9")
            long successCount, // 성공 건수
            @Schema(description = "실패 건수", example = "1")
            long failureCount // 실패 건수
    ) {
    }
}
