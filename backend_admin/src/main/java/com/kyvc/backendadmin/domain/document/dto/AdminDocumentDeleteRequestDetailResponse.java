package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문서 삭제 요청 상세 응답")
public record AdminDocumentDeleteRequestDetailResponse(
        @Schema(description = "삭제 요청 ID", example = "1")
        Long requestId,
        @Schema(description = "문서 ID", example = "100")
        Long documentId,
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId,
        @Schema(description = "법인 ID", example = "20")
        Long corporateId,
        @Schema(description = "법인명", example = "KYVC 테스트 법인")
        String corporateName,
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode,
        @Schema(description = "파일명", example = "business-registration.pdf")
        String fileName,
        @Schema(description = "파일 경로. 현재 보안 정책에 따라 null로 응답합니다.")
        String filePath,
        @Schema(description = "요청 사유", example = "잘못 업로드한 문서입니다.")
        String requestReason,
        @Schema(description = "요청 상태 코드", example = "REQUESTED")
        String requestStatusCode,
        @Schema(description = "요청 사용자 ID", example = "100")
        Long requestedByUserId,
        @Schema(description = "요청 사용자 이메일", example = "user@kyvc.local")
        String requestedByUserEmail,
        @Schema(description = "처리 관리자 ID", example = "1")
        Long processedByAdminId,
        @Schema(description = "처리 관리자명", example = "Backend Admin")
        String processedByAdminName,
        @Schema(description = "처리 사유", example = "삭제 요청 사유가 타당하여 승인합니다.")
        String processedReason,
        @Schema(description = "요청 일시")
        LocalDateTime requestedAt,
        @Schema(description = "처리 일시")
        LocalDateTime processedAt
) {
}
