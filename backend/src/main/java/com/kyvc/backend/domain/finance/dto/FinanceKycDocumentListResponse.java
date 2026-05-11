package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 금융사 방문 KYC 문서 목록 응답
 *
 * @param items 문서 목록
 */
@Schema(description = "금융사 방문 KYC 문서 목록 응답")
public record FinanceKycDocumentListResponse(
        @Schema(description = "문서 목록")
        List<Item> items // 문서 목록
) {

    /**
     * 금융사 방문 KYC 문서 목록 항목
     *
     * @param documentId 문서 ID
     * @param kycId KYC 신청 ID
     * @param documentTypeCode 문서 유형 코드
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param status 문서 상태 코드
     * @param uploadedByTypeCode 업로드 주체 유형 코드
     * @param uploadedAt 업로드 일시
     */
    @Schema(description = "금융사 방문 KYC 문서 목록 항목")
    public record Item(
            @Schema(description = "문서 ID", example = "1")
            Long documentId, // 문서 ID
            @Schema(description = "KYC 신청 ID", example = "1")
            Long kycId, // KYC 신청 ID
            @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
            String documentTypeCode, // 문서 유형 코드
            @Schema(description = "파일명", example = "business.pdf")
            String fileName, // 파일명
            @Schema(description = "파일 크기", example = "12345")
            Long fileSize, // 파일 크기
            @Schema(description = "문서 상태 코드", example = "UPLOADED")
            String status, // 문서 상태 코드
            @Schema(description = "업로드 주체 유형 코드", example = "FINANCE")
            String uploadedByTypeCode, // 업로드 주체 유형 코드
            @Schema(description = "업로드 일시", example = "2026-05-11T10:00:00")
            LocalDateTime uploadedAt // 업로드 일시
    ) {
    }
}
