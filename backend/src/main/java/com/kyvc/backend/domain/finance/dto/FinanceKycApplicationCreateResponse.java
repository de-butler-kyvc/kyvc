package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 생성 응답
 *
 * @param kycId KYC 신청 ID
 * @param status KYC 상태 코드
 * @param applicationChannelCode 신청 채널 코드
 * @param corporateId 법인 ID
 */
@Schema(description = "금융사 방문 KYC 생성 응답")
public record FinanceKycApplicationCreateResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태 코드", example = "DRAFT")
        String status, // KYC 상태 코드
        @Schema(description = "신청 채널 코드", example = "FINANCE_VISIT")
        String applicationChannelCode, // 신청 채널 코드
        @Schema(description = "법인 ID", example = "1")
        Long corporateId // 법인 ID
) {
}
