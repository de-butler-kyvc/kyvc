package com.kyvc.backendadmin.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

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
        CoreRequestSummary coreRequestSummary,

        /** 최근 KYC 신청 목록 */
        @Schema(description = "최근 KYC 신청 목록")
        List<RecentKyc> recentKycs
) {

    /**
     * 최근 KYC 신청 요약 DTO입니다.
     */
    @Schema(description = "최근 KYC 신청 요약 정보")
    public record RecentKyc(
            /** KYC 신청 ID */
            @Schema(description = "KYC 신청 ID", example = "100")
            Long kycId,

            /** 법인명 */
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName,

            /** KYC 신청 상태 */
            @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
            String kycStatus,

            /** 제출 시각 */
            @Schema(description = "제출 시각")
            LocalDateTime submittedAt
    ) {
    }

    /**
     * KYC 신청 상태 집계 DTO입니다.
     */
    @Schema(description = "KYC 신청 상태 집계")
    public record KycSummary(
            /** 전체 KYC 신청 수 */
            @Schema(description = "전체 KYC 신청 수", example = "120")
            long total,

            /** 임시저장 수 */
            @Schema(description = "임시저장 수", example = "3")
            long draft,

            /** 제출 완료 수 */
            @Schema(description = "제출 완료 수", example = "20")
            long submitted,

            /** AI 심사 중 수 */
            @Schema(description = "AI 심사 중 수", example = "10")
            long aiReviewing,

            /** 수동심사 대기 수 */
            @Schema(description = "수동심사 대기 수", example = "8")
            long manualReview,

            /** 보완요청 수 */
            @Schema(description = "보완요청 수", example = "6")
            long needSupplement,

            /** 승인 수 */
            @Schema(description = "승인 수", example = "70")
            long approved,

            /** 반려 수 */
            @Schema(description = "반려 수", example = "6")
            long rejected,

            /** VC 발급 완료 수 */
            @Schema(description = "VC 발급 완료 수", example = "13")
            long vcIssued
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

            /** VC 유효 수 */
            @Schema(description = "VC 유효 수", example = "60")
            long valid,

            /** VC 만료 수 */
            @Schema(description = "VC 만료 수", example = "2")
            long expired,

            /** VC 폐기 수 */
            @Schema(description = "VC 폐기 수", example = "1")
            long revoked,

            /** VC 일시중지 수 */
            @Schema(description = "VC 일시중지 수", example = "1")
            long suspended
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

            /** Core 요청 접수 수 */
            @Schema(description = "Core 요청 접수 수", example = "4")
            long requested,

            /** Core 요청 처리 중 수 */
            @Schema(description = "Core 요청 처리 중 수", example = "5")
            long processing,

            /** Core 요청 성공 수 */
            @Schema(description = "Core 요청 성공 수", example = "90")
            long success,

            /** Core 요청 실패 수 */
            @Schema(description = "Core 요청 실패 수", example = "3")
            long failed,

            /** Core callback 수신 수 */
            @Schema(description = "Core callback 수신 수", example = "2")
            long callbackReceived,

            /** Core 재시도 중 수 */
            @Schema(description = "Core 재시도 중 수", example = "1")
            long retrying
    ) {
    }
}
