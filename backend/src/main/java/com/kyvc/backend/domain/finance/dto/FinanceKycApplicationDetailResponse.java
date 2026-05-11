package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 금융사 방문 KYC 상세 응답
 *
 * @param kycId KYC 신청 ID
 * @param status KYC 상태 코드
 * @param applicationChannelCode 신청 채널 코드
 * @param corporate 법인 요약
 * @param documents 제출서류 요약 목록
 */
@Schema(description = "금융사 방문 KYC 상세 응답")
public record FinanceKycApplicationDetailResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태 코드", example = "DRAFT")
        String status, // KYC 상태 코드
        @Schema(description = "신청 채널 코드", example = "FINANCE_VISIT")
        String applicationChannelCode, // 신청 채널 코드
        @Schema(description = "법인 요약")
        CorporateSummary corporate, // 법인 요약
        @Schema(description = "제출서류 요약 목록")
        List<DocumentSummary> documents // 제출서류 요약 목록
) {

    /**
     * 법인 요약
     *
     * @param corporateId 법인 ID
     * @param corporateName 법인명
     */
    @Schema(description = "법인 요약")
    public record CorporateSummary(
            @Schema(description = "법인 ID", example = "1")
            Long corporateId, // 법인 ID
            @Schema(description = "법인명", example = "주식회사 KYVC")
            String corporateName // 법인명
    ) {
    }

    /**
     * 제출서류 요약
     *
     * @param documentId 문서 ID
     * @param documentTypeCode 문서 유형 코드
     * @param fileName 파일명
     * @param uploadStatusCode 업로드 상태 코드
     * @param uploadedAt 업로드 일시
     */
    @Schema(description = "제출서류 요약")
    public record DocumentSummary(
            @Schema(description = "문서 ID", example = "1")
            Long documentId, // 문서 ID
            @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
            String documentTypeCode, // 문서 유형 코드
            @Schema(description = "파일명", example = "business-registration.pdf")
            String fileName, // 파일명
            @Schema(description = "업로드 상태 코드", example = "UPLOADED")
            String uploadStatusCode, // 업로드 상태 코드
            @Schema(description = "업로드 일시", example = "2026-05-11T10:00:00")
            LocalDateTime uploadedAt // 업로드 일시
    ) {
    }
}
