package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 원본서류 삭제 요청 목록 항목
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
        @Schema(description = "법인명", example = "테스트 주식회사")
        String corporateName,

        /** 문서 유형 코드 */
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode,

        /** 요청 사용자 ID */
        @Schema(description = "요청 사용자 ID", example = "40")
        Long requestedByUserId,

        /** 요청 사유 */
        @Schema(description = "요청 사유")
        String reason,

        /** 삭제 요청 상태 */
        @Schema(description = "삭제 요청 상태", example = "REQUESTED")
        String status,

        /** 요청 일시 */
        @Schema(description = "요청 일시", example = "2026-05-10T10:30:00")
        String requestedAt,

        /** 처리 관리자 ID */
        @Schema(description = "처리 관리자 ID", example = "1")
        Long processedByAdminId,

        /** 처리 일시 */
        @Schema(description = "처리 일시", example = "2026-05-10T11:00:00")
        String processedAt
) {
}
