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

            /** 불일치 유형 코드 */
            @Schema(description = "불일치 유형 코드", example = "CORPORATE_NAME_MISMATCH")
            String mismatchTypeCode,

            /** 불일치 유형명 */
            @Schema(description = "불일치 유형명", example = "법인명 불일치")
            String mismatchTypeName,

            /** 기준 문서 유형 코드 */
            @Schema(description = "기준 문서 유형 코드", example = "BUSINESS_REGISTRATION")
            String sourceDocumentTypeCode,

            /** 비교 문서 유형 코드 */
            @Schema(description = "비교 문서 유형 코드", example = "CORPORATE_REGISTRATION")
            String targetDocumentTypeCode,

            /** 기준 문서 값 */
            @Schema(description = "기준 문서 값", example = "케이와이브이씨")
            String sourceValue,

            /** 비교 문서 값 */
            @Schema(description = "비교 문서 값", example = "케이와이브이씨 주식회사")
            String targetValue,

            /** 일치 여부 */
            @Schema(description = "일치 여부", example = "N")
            String matchedYn,

            /** 신뢰도 점수 */
            @Schema(description = "신뢰도 점수", example = "88.30")
            BigDecimal confidenceScore,

            /** 판단 사유 */
            @Schema(description = "판단 사유", example = "문서 간 법인명 표기가 다릅니다.")
            String reason
    ) {
    }
}
