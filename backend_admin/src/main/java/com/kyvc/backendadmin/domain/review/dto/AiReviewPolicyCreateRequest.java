package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * AI 심사 정책 등록 요청 DTO입니다.
 */
@Schema(description = "AI 심사 정책 등록 요청")
public record AiReviewPolicyCreateRequest(

        /** 정책명 */
        @NotBlank(message = "policyName은 필수입니다.")
        @Schema(description = "정책명", example = "법인 기본 AI 심사 정책")
        String policyName,

        /** 법인 유형 코드 */
        @NotBlank(message = "corporateTypeCode는 필수입니다.")
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode,

        /** 자동 승인 사용 여부 */
        @NotBlank(message = "autoApproveYn은 필수입니다.")
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

        /** 정책 설명 */
        @Schema(description = "정책 설명", example = "법인 기본 KYC AI 심사 업무 정책")
        String description
) {
}
