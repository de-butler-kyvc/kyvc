package com.kyvc.backendadmin.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영 리포트 DTO 모음입니다.
 */
public final class AdminOperationReportDtos {
    private AdminOperationReportDtos() {
    }

    @Schema(description = "운영 리포트 응답")
    public record Response(
            /** 조회 시작일 */
            @Schema(description = "조회 시작일")
            LocalDate fromDate,
            /** 조회 종료일 */
            @Schema(description = "조회 종료일")
            LocalDate toDate,
            /** 그룹 기준 */
            @Schema(description = "그룹 기준", example = "DAILY")
            String groupBy,
            /** 요약 집계 */
            @Schema(description = "요약 집계")
            Summary summary,
            /** 기간별 집계 */
            @Schema(description = "기간별 집계")
            List<Row> rows
    ) {
    }

    @Schema(description = "운영 리포트 요약")
    public record Summary(long kycApplications, long kycApproved, long kycRejected, long supplementRequested,
                          long aiReviewSuccess, long aiReviewFailed, long manualReviewPending,
                          long vcIssueRequested, long vcIssueSuccess, long vpVerificationRequested,
                          long vpVerificationSuccess, long vpVerificationFailed, long verifierApiCalls,
                          long coreRequestFailed) {
    }

    @Schema(description = "운영 리포트 기간별 행")
    public record Row(
            /** 집계 기간 라벨 */
            @Schema(description = "집계 기간 라벨", example = "2026-05-09")
            String period,
            long kycApplications, long kycApproved, long kycRejected, long supplementRequested,
            long aiReviewSuccess, long aiReviewFailed, long manualReviewPending,
            long vcIssueRequested, long vcIssueSuccess, long vpVerificationRequested,
            long vpVerificationSuccess, long vpVerificationFailed, long verifierApiCalls,
            long coreRequestFailed
    ) {
    }

    @Schema(description = "운영 리포트 Export 응답")
    public record ExportResponse(
            /** 파일명 */
            @Schema(description = "파일명", example = "operations-report.csv")
            String fileName,
            /** CSV 컨텐츠 */
            @Schema(description = "CSV 컨텐츠")
            String content
    ) {
    }
}
