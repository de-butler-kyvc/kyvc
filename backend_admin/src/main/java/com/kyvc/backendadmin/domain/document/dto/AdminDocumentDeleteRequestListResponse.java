package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문서 삭제 요청 목록 응답")
public record AdminDocumentDeleteRequestListResponse(
        @Schema(description = "문서 삭제 요청 목록")
        List<Item> items,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "100")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
    @Schema(description = "문서 삭제 요청 목록 항목")
    public record Item(
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
            @Schema(description = "요청 사용자 ID", example = "100")
            Long requestedByUserId,
            @Schema(description = "요청 사용자 이메일", example = "user@kyvc.local")
            String requestedByUserEmail,
            @Schema(description = "요청 상태 코드", example = "REQUESTED")
            String requestStatusCode,
            @Schema(description = "요청 일시")
            LocalDateTime requestedAt,
            @Schema(description = "처리 일시")
            LocalDateTime processedAt
    ) {
    }
}
