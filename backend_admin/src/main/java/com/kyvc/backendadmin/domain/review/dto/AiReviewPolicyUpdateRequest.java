package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * AI 심사 정책 수정 요청 DTO입니다.
 */
@Schema(description = "AI 심사 정책 수정 요청")
public record AiReviewPolicyUpdateRequest(

        /** 정책명 */
        @Schema(description = "정책명", example = "법인 기본 AI 심사 정책 v2")
        String policyName,

        /** 법인 유형 코드 */
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode,

        /** 자동 승인 사용 여부 */
        @Schema(description = "자동 승인 사용 여부(Y/N)", example = "N")
        String autoApproveYn,

        /** 자동 승인 기준 threshold */
        @Schema(description = "자동 승인 기준 threshold", example = "0.95")
        BigDecimal autoApproveThreshold,

        /** 수동 심사 전환 기준 threshold */
        @Schema(description = "수동 심사 전환 기준 threshold", example = "0.85")
        BigDecimal manualReviewThreshold,

        /** 보완요청 후보 기준 threshold */
        @Schema(description = "보완요청 후보 기준 threshold", example = "0.75")
        BigDecimal supplementThreshold,

        /** 정책 설명 */
        @Schema(description = "정책 설명", example = "정책 기준 조정")
        String description
) {
}
