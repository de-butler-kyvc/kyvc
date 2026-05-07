package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 심사 정책 목록 조회 응답 DTO입니다.
 */
@Schema(description = "AI 심사 정책 목록 조회 응답")
public record AiReviewPolicySummaryResponse(

        /** AI 심사 정책 목록 */
        @Schema(description = "AI 심사 정책 목록")
        List<Item> items,

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

    /**
     * AI 심사 정책 목록 항목 DTO입니다.
     */
    @Schema(description = "AI 심사 정책 목록 항목")
    public record Item(

            /** AI 심사 정책 ID */
            @Schema(description = "AI 심사 정책 ID", example = "1")
            Long aiPolicyId,

            /** 정책명 */
            @Schema(description = "정책명", example = "법인 기본 AI 심사 정책")
            String policyName,

            /** 법인 유형 코드 */
            @Schema(description = "법인 유형 코드", example = "CORPORATION")
            String corporateTypeCode,

            /** 자동 승인 사용 여부 */
            @Schema(description = "자동 승인 사용 여부(Y/N)", example = "Y")
            String autoApproveYn,

            /** 자동 승인 기준 threshold */
            @Schema(description = "자동 승인 기준 threshold", example = "0.90")
            BigDecimal autoApproveThreshold,

            /** 수동 심사 전환 기준 threshold */
            @Schema(description = "수동 심사 전환 기준 threshold", example = "0.80")
            BigDecimal manualReviewThreshold,

            /** 보완요청 후보 기준 threshold */
            @Schema(description = "보완요청 후보 기준 threshold", example = "0.70")
            BigDecimal supplementThreshold,

            /** 사용 여부 */
            @Schema(description = "사용 여부(Y/N)", example = "Y")
            String enabledYn,

            /** 정책 상태 */
            @Schema(description = "정책 상태", example = "ACTIVE")
            String status,

            /** 수정 시각 */
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }
}
