package com.kyvc.backendadmin.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Backend Admin 대시보드 집계 응답 DTO입니다.
 */
@Schema(description = "Backend Admin 대시보드 집계 응답")
public record AdminDashboardResponse(

        /** KYC 신청 상태 집계 */
        @Schema(description = "KYC 신청 상태 집계")
        KycSummary kycSummary,

        /** AI 심사 상태 집계 */
        @Schema(description = "AI 심사 상태 집계")
        AiReviewSummary aiReviewSummary,

        /** VC 발급 상태 집계 */
        @Schema(description = "VC 발급 상태 집계")
        VcSummary vcSummary,

        /** Core 요청 상태 집계 */
        @Schema(description = "Core 요청 상태 집계")
        CoreRequestSummary coreRequestSummary
) {

    /**
     * KYC 신청 상태 집계 DTO입니다.
     */
    @Schema(description = "KYC 신청 상태 집계")
    public record KycSummary(

            /** 전체 KYC 신청 수 */
            @Schema(description = "전체 KYC 신청 수", example = "120")
            long total,

            /** 제출 완료 수 */
            @Schema(description = "제출 완료 수", example = "20")
            long submitted,

            /** AI 심사 중 수 */
            @Schema(description = "AI 심사 중 수", example = "10")
            long aiReviewing,

            /** 수동심사 필요 수 */
            @Schema(description = "수동심사 필요 수", example = "8")
            long manualReview,

            /** 보완요청 수 */
            @Schema(description = "보완요청 수", example = "6")
            long needSupplement,

            /** 승인 수 */
            @Schema(description = "승인 수", example = "70")
            long approved,

            /** 반려 수 */
            @Schema(description = "반려 수", example = "6")
            long rejected
    ) {
    }

    /**
     * AI 심사 상태 집계 DTO입니다.
     */
    @Schema(description = "AI 심사 상태 집계")
    public record AiReviewSummary(

            /** 대기 수 */
            @Schema(description = "대기 수", example = "5")
            long queued,

            /** 실행 중 수 */
            @Schema(description = "실행 중 수", example = "3")
            long running,

            /** 성공 수 */
            @Schema(description = "성공 수", example = "80")
            long success,

            /** 실패 수 */
            @Schema(description = "실패 수", example = "4")
            long failed,

            /** 낮은 신뢰도 수 */
            @Schema(description = "낮은 신뢰도 수", example = "9")
            long lowConfidence
    ) {
    }

    /**
     * VC 발급 상태 집계 DTO입니다.
     */
    @Schema(description = "VC 발급 상태 집계")
    public record VcSummary(

            /** VC 발급 중 수 */
            @Schema(description = "VC 발급 중 수", example = "4")
            long issuing,

            /** VC 발급 완료 수 */
            @Schema(description = "VC 발급 완료 수", example = "60")
            long valid,

            /** VC 발급 실패 수 */
            @Schema(description = "VC 발급 실패 수", example = "2")
            long failed,

            /** VC 폐기 수 */
            @Schema(description = "VC 폐기 수", example = "1")
            long revoked
    ) {
    }

    /**
     * Core 요청 상태 집계 DTO입니다.
     */
    @Schema(description = "Core 요청 상태 집계")
    public record CoreRequestSummary(

            /** Core 요청 대기 수 */
            @Schema(description = "Core 요청 대기 수", example = "7")
            long queued,

            /** Core 요청 처리 중 수 */
            @Schema(description = "Core 요청 처리 중 수", example = "5")
            long processing,

            /** Core 요청 성공 수 */
            @Schema(description = "Core 요청 성공 수", example = "90")
            long success,

            /** Core 요청 실패 수 */
            @Schema(description = "Core 요청 실패 수", example = "3")
            long failed
    ) {
    }
}
