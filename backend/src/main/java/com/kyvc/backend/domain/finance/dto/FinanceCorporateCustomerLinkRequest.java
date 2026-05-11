package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 법인 고객 연결 요청
 *
 * @param financeInstitutionCode 금융기관 코드
 * @param financeBranchCode 금융사 지점 코드
 * @param financeCustomerNo 금융사 고객번호
 * @param corporateId 법인 ID
 */
@Schema(description = "금융사 법인 고객 연결 요청")
public record FinanceCorporateCustomerLinkRequest(
        @Schema(description = "금융기관 코드", example = "FINANCE_USER_1")
        String financeInstitutionCode, // 금융기관 코드
        @Schema(description = "금융사 지점 코드", example = "BR001", nullable = true)
        String financeBranchCode, // 금융사 지점 코드
        @Schema(description = "금융사 고객번호", example = "CUST-001")
        String financeCustomerNo, // 금융사 고객번호
        @Schema(description = "법인 ID", example = "1")
        Long corporateId // 법인 ID
) {
}
