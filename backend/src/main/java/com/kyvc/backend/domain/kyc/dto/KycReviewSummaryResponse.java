package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 심사 결과 요약 응답
 *
 * @param kycId KYC 요청 ID
 * @param kycStatus KYC 상태
 * @param aiReviewStatus AI 심사 상태
 * @param aiReviewResult AI 심사 결과
 * @param confidenceScore 신뢰도 점수
 * @param summaryMessage 요약 메시지
 * @param findings 심사 결과 항목 목록
 * @param manualReviewRequired 수동심사 필요 여부
 * @param reviewedAt 심사 반영 일시
 */
@Schema(description = "KYC 심사 결과 요약 응답")
public record KycReviewSummaryResponse(
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "KYC 상태", example = "APPROVED")
        String kycStatus, // KYC 상태
        @Schema(description = "AI 심사 상태", example = "SUCCESS")
        String aiReviewStatus, // AI 심사 상태
        @Schema(description = "AI 심사 결과", example = "PASS")
        String aiReviewResult, // AI 심사 결과
        @Schema(description = "신뢰도 점수", example = "95.50")
        BigDecimal confidenceScore, // 신뢰도 점수
        @Schema(description = "요약 메시지", example = "AI 심사 결과 주요 제출서류가 모두 확인되었습니다.")
        String summaryMessage, // 요약 메시지
        @Schema(description = "심사 결과 항목 목록")
        List<KycReviewFindingResponse> findings, // 심사 결과 항목 목록
        @Schema(description = "수동심사 필요 여부", example = "false")
        Boolean manualReviewRequired, // 수동심사 필요 여부
        @Schema(description = "심사 반영 일시", example = "2026-05-05T14:00:00")
        LocalDateTime reviewedAt // 심사 반영 일시
) {

    public KycReviewSummaryResponse {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
