package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 금융사 VP 요청 결과 응답
 *
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param verifiedAt 검증 일시
 */
@Schema(description = "금융사 VP 요청 결과 응답")
public record FinanceVpRequestResultResponse(
        @Schema(description = "법인명", example = "주식회사 KYVC")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "검증 일시", example = "2026-05-11T10:00:00")
        LocalDateTime verifiedAt // 검증 일시
) {
}
