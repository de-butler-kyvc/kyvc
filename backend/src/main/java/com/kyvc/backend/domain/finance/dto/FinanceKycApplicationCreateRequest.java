package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 생성 요청
 *
 * @param corporateId 법인 ID
 * @param financeCustomerNo 금융사 고객번호
 * @param financeBranchCode 금융사 지점 코드
 * @param corporateTypeCode 법인 유형 코드
 */
@Schema(description = "금융사 방문 KYC 생성 요청")
public record FinanceKycApplicationCreateRequest(
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "금융사 고객번호", example = "CUST-001")
        String financeCustomerNo, // 금융사 고객번호
        @Schema(description = "금융사 지점 코드", example = "BR001")
        String financeBranchCode, // 금융사 지점 코드
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode // 법인 유형 코드
) {
}
