package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * KYC 누락 항목 응답
 *
 * @param code 누락 항목 코드
 * @param message 누락 항목 설명
 * @param target 누락 대상 식별값
 */
@Schema(description = "KYC 누락 항목 응답")
public record KycMissingItemResponse(
        @Schema(description = "누락 항목 코드", example = "DOCUMENT_REQUIRED")
        String code, // 누락 항목 코드
        @Schema(description = "누락 항목 설명", example = "사업자등록증 업로드가 필요합니다.")
        String message, // 누락 항목 설명
        @Schema(description = "누락 대상 식별값", example = "BUSINESS_REGISTRATION")
        String target // 누락 대상 식별값
) {
}
