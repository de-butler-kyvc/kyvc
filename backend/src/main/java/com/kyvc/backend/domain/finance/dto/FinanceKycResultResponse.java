package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 심사 결과 응답
 *
 * @param kycId KYC 신청 ID
 * @param status KYC 상태 코드
 * @param aiReviewStatus AI 심사 상태 코드
 * @param summary 심사 결과 요약
 * @param manualReviewRequiredYn 수동심사 필요 여부
 * @param supplementRequiredYn 보완 필요 여부
 * @param credentialIssuableYn Credential 발급 가능 여부
 * @param credentialStatus Credential 상태 코드
 */
@Schema(description = "금융사 방문 KYC 심사 결과 응답")
public record FinanceKycResultResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태 코드", example = "APPROVED")
        String status, // KYC 상태 코드
        @Schema(description = "AI 심사 상태 코드", example = "SUCCESS")
        String aiReviewStatus, // AI 심사 상태 코드
        @Schema(description = "심사 결과 요약", example = "AI 심사 결과 요약")
        String summary, // 심사 결과 요약
        @Schema(description = "수동심사 필요 여부", example = "N")
        String manualReviewRequiredYn, // 수동심사 필요 여부
        @Schema(description = "보완 필요 여부", example = "N")
        String supplementRequiredYn, // 보완 필요 여부
        @Schema(description = "Credential 발급 가능 여부", example = "Y")
        String credentialIssuableYn, // Credential 발급 가능 여부
        @Schema(description = "Credential 상태 코드", example = "VALID", nullable = true)
        String credentialStatus // Credential 상태 코드
) {
}
