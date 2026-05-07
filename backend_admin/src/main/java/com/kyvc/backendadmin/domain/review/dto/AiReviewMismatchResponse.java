package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 문서 불일치 결과 조회 응답 DTO입니다.
 */
@Schema(description = "AI 문서 불일치 결과 조회 응답")
public record AiReviewMismatchResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** AI가 판단한 문서 간 불일치 항목 목록 */
        @Schema(description = "AI가 판단한 문서 간 불일치 항목 목록")
        List<Mismatch> mismatches
) {

    /**
     * AI 문서 불일치 항목 DTO입니다.
     */
    @Schema(description = "AI 문서 불일치 항목")
    public record Mismatch(

            /** 불일치 필드명 */
            @Schema(description = "불일치 필드명", example = "representativeName")
            String fieldName,

            /** 비교 원천 A */
            @Schema(description = "비교 원천 A", example = "BUSINESS_REGISTRATION")
            String sourceA,

            /** 비교 값 A */
            @Schema(description = "비교 값 A", example = "홍길동")
            String valueA,

            /** 비교 원천 B */
            @Schema(description = "비교 원천 B", example = "CORPORATE_REGISTRATION")
            String sourceB,

            /** 비교 값 B */
            @Schema(description = "비교 값 B", example = "김길동")
            String valueB,

            /** 불일치 심각도 */
            @Schema(description = "불일치 심각도", example = "HIGH")
            String severity,

            /** 판단 신뢰도 */
            @Schema(description = "판단 신뢰도", example = "88.30")
            BigDecimal confidence
    ) {
    }
}
