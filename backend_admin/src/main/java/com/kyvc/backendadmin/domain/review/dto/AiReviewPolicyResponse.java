package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 심사 정책 상세 응답 DTO입니다.
 */
@Schema(description = "AI 심사 정책 상세 응답")
public record AiReviewPolicyResponse(

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

        /** 정책 설명 */
        @Schema(description = "정책 설명", example = "법인 기본 KYC AI 심사 업무 정책")
        String description,

        /** 적용 시작 시각 */
        @Schema(description = "적용 시작 시각")
        LocalDateTime effectiveFrom,

        /** 적용 종료 시각 */
        @Schema(description = "적용 종료 시각")
        LocalDateTime effectiveTo,

        /** 생성 관리자 ID */
        @Schema(description = "생성 관리자 ID", example = "1")
        Long createdByAdminId,

        /** 수정 관리자 ID */
        @Schema(description = "수정 관리자 ID", example = "1")
        Long updatedByAdminId,

        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
