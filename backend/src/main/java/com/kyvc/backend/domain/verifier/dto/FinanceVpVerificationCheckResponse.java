package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 VP 검증 항목 응답
 *
 * @param checkType 검증 항목 유형
 * @param checkName 검증 항목명
 * @param resultCode 검증 결과 코드
 * @param message 검증 결과 메시지
 */
@Schema(description = "금융사 VP 검증 항목 응답")
public record FinanceVpVerificationCheckResponse(
        @Schema(description = "검증 항목 유형", example = "VP_FORMAT")
        String checkType, // 검증 항목 유형
        @Schema(description = "검증 항목명", example = "VP 형식 검증")
        String checkName, // 검증 항목명
        @Schema(description = "검증 결과 코드", example = "PASSED")
        String resultCode, // 검증 결과 코드
        @Schema(description = "검증 결과 메시지", example = "VP 형식이 유효합니다.")
        String message // 검증 결과 메시지
) {
}
