package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 문서 업로드 응답
 *
 * @param documentId 문서 ID
 * @param kycId KYC 신청 ID
 * @param documentTypeCode 문서 유형 코드
 * @param status 문서 상태 코드
 */
@Schema(description = "금융사 방문 KYC 문서 업로드 응답")
public record FinanceKycDocumentUploadResponse(
        @Schema(description = "문서 ID", example = "1")
        Long documentId, // 문서 ID
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode, // 문서 유형 코드
        @Schema(description = "문서 상태 코드", example = "UPLOADED")
        String status // 문서 상태 코드
) {
}
