package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 제출 응답
 *
 * @param kycId KYC 신청 ID
 * @param status KYC 상태 코드
 * @param aiReviewStatus AI 심사 상태 코드
 * @param manualReviewRequiredYn 수동심사 필요 여부
 */
@Schema(description = "금융사 방문 KYC 제출 응답")
public record FinanceKycSubmitResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태 코드", example = "APPROVED")
        String status, // KYC 상태 코드
        @Schema(description = "AI 심사 상태 코드", example = "SUCCESS")
        String aiReviewStatus, // AI 심사 상태 코드
        @Schema(description = "수동심사 필요 여부", example = "N")
        String manualReviewRequiredYn // 수동심사 필요 여부
) {
}
