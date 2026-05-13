package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 법인 고객 연결 응답
 *
 * @param linked 연결 여부
 * @param financeCustomerId 금융사 고객 연결 ID
 * @param corporateId 법인 ID
 * @param financeCustomerNo 금융사 고객번호
 * @param statusCode 연결 상태 코드
 */
@Schema(description = "금융사 법인 고객 연결 응답")
public record FinanceCorporateCustomerLinkResponse(
        @Schema(description = "연결 여부", example = "true")
        boolean linked, // 연결 여부
        @Schema(description = "금융사 고객 연결 ID", example = "1")
        Long financeCustomerId, // 금융사 고객 연결 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "금융사 고객번호", example = "CUST-001")
        String financeCustomerNo, // 금융사 고객번호
        @Schema(description = "연결 상태 코드", example = "ACTIVE")
        String statusCode // 연결 상태 코드
) {
}
