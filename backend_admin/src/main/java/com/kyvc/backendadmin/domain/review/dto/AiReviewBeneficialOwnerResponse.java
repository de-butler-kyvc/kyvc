package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 실제소유자 판단 결과 조회 응답 DTO입니다.
 */
@Schema(description = "AI 실제소유자 판단 결과 조회 응답")
public record AiReviewBeneficialOwnerResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** AI가 판단한 실제소유자 후보 목록 */
        @Schema(description = "AI가 판단한 실제소유자 후보 목록")
        List<BeneficialOwner> beneficialOwners
) {

    /**
     * AI 실제소유자 판단 항목 DTO입니다.
     */
    @Schema(description = "AI 실제소유자 판단 항목")
    public record BeneficialOwner(

            /** 소유자명 */
            @Schema(description = "소유자명", example = "홍길동")
            String ownerName,

            /** 지분율 */
            @Schema(description = "지분율", example = "35.50")
            BigDecimal ownershipRatio,

            /** 지배 유형 코드 */
            @Schema(description = "지배 유형 코드", example = "SHAREHOLDER")
            String controlTypeCode,

            /** 실제소유자 여부 */
            @Schema(description = "실제소유자 여부", example = "Y")
            String beneficialOwnerYn,

            /** 신뢰도 점수 */
            @Schema(description = "신뢰도 점수", example = "91.20")
            BigDecimal confidenceScore,

            /** 판단 사유 */
            @Schema(description = "판단 사유", example = "25% 이상 지분을 보유했습니다.")
            String judgementReason
    ) {
    }
}
