package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 원본서류 삭제 요청 목록 항목 응답입니다.
 */
@Schema(description = "원본서류 삭제 요청 목록 항목")
public record DocumentDeleteRequestSummaryResponse(
        /** 삭제 요청 ID */
        @Schema(description = "삭제 요청 ID", example = "1")
        Long requestId,

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId,

        /** 문서 ID */
        @Schema(description = "문서 ID", example = "20")
        Long documentId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "30")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "모의 주식회사")
        String corporateName,

        /** 문서 유형 코드 */
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode,

        /** 원본 파일명 */
        @Schema(description = "원본 파일명", example = "business-registration.pdf")
        String originalFileName,

        /** 삭제 요청 상태 */
        @Schema(description = "삭제 요청 상태", example = "REQUESTED")
        String status,

        /** 요청 일시 */
        @Schema(description = "요청 일시", example = "2026-05-10T10:30:00")
        String requestedAt
) {
}
