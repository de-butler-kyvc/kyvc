package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * KYC 심사 결과 항목 응답
 *
 * @param findingType 심사 항목 유형
 * @param result 심사 결과
 * @param message 심사 메시지
 * @param confidenceScore 신뢰도 점수
 */
@Schema(description = "KYC 심사 결과 항목 응답")
public record KycReviewFindingResponse(
        @Schema(description = "심사 항목 유형", example = "SUMMARY")
        String findingType, // 심사 항목 유형
        @Schema(description = "심사 결과", example = "PASS")
        String result, // 심사 결과
        @Schema(description = "심사 메시지", example = "AI 심사 결과 주요 제출서류가 모두 확인되었습니다.")
        String message, // 심사 메시지
        @Schema(description = "신뢰도 점수", example = "95.50")
        BigDecimal confidenceScore // 신뢰도 점수
) {
}
