package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 금융사 직원 컨텍스트 응답
 *
 * @param financeInstitutionCode 금융기관 코드
 * @param branchCode 지점 코드
 * @param roles 보유 역할 코드 목록
 */
@Schema(description = "금융사 직원 컨텍스트 응답")
public record FinanceMeResponse(
        @Schema(description = "금융기관 코드", example = "FINANCE_USER_1")
        String financeInstitutionCode, // 금융기관 코드
        @Schema(description = "지점 코드", example = "BR001", nullable = true)
        String branchCode, // 지점 코드
        @Schema(description = "보유 역할 코드 목록", example = "[\"ROLE_FINANCE_STAFF\"]")
        List<String> roles // 보유 역할 코드 목록
) {
}
