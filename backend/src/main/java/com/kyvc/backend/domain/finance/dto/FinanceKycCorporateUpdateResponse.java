package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 법인정보 수정 응답
 *
 * @param updated 수정 여부
 * @param corporateId 법인 ID
 * @param kycId KYC 신청 ID
 */
@Schema(description = "금융사 방문 KYC 법인정보 수정 응답")
public record FinanceKycCorporateUpdateResponse(
        @Schema(description = "수정 여부", example = "true")
        boolean updated, // 수정 여부
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId // KYC 신청 ID
) {
}
