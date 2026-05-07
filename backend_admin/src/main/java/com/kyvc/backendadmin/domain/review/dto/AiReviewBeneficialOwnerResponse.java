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
            BigDecimal shareRatio,

            /** 실제소유자 여부 */
            @Schema(description = "실제소유자 여부", example = "true")
            Boolean isBeneficialOwner,

            /** 판단 근거 */
            @Schema(description = "판단 근거", example = "25% 이상 지분 보유")
            String basis,

            /** 판단 신뢰도 */
            @Schema(description = "판단 신뢰도", example = "91.20")
            BigDecimal confidence
    ) {
    }
}
