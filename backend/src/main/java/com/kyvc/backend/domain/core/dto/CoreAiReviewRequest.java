package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core AI 심사 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param kycId KYC 요청 ID
 * @param corporateId 법인 ID
 * @param businessNumber 사업자등록번호
 * @param corporateRegistrationNumber 법인등록번호
 * @param corporateName 법인명
 * @param representativeName 대표자명
 * @param representativePhone 대표자 연락처
 * @param representativeEmail 대표자 이메일
 * @param agentName 대리인명
 * @param corporateTypeCode 법인 유형 코드
 * @param documents 심사 문서 목록
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core AI 심사 요청")
public record CoreAiReviewRequest(
        @Schema(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessNumber, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNumber, // 법인등록번호
        @Schema(description = "법인명", example = "KYVC Corp")
        String corporateName, // 법인명
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName, // 대표자명
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "ceo@kyvc.com")
        String representativeEmail, // 대표자 이메일
        @Schema(description = "대리인명", example = "김대리")
        String agentName, // 대리인명
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "심사 문서 목록")
        List<CoreAiReviewDocumentRequest> documents, // 심사 문서 목록
        @Schema(description = "요청 시각", example = "2026-05-06T10:00:00")
        LocalDateTime requestedAt // 요청 시각
) {

    public CoreAiReviewRequest {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    /**
     * AI 심사 문서 요청
     *
     * @param documentId 문서 ID
     * @param documentTypeCode 문서 유형 코드
     * @param documentHash 문서 해시
     * @param originalFileName 원본 파일명
     * @param mimeType MIME 유형
     * @param storagePath 저장 경로
     * @param contentBase64 문서 원문 Base64
     * @param fileSize 파일 크기
     */
    @Schema(description = "AI 심사 문서 요청")
    public record CoreAiReviewDocumentRequest(
            @Schema(description = "문서 ID", example = "101")
            Long documentId, // 문서 ID
            @Schema(description = "문서 유형 코드", example = "BUSINESS_LICENSE")
            String documentTypeCode, // 문서 유형 코드
            @Schema(description = "문서 해시", example = "sha256:abcdef123456")
            String documentHash, // 문서 해시
            @Schema(description = "원본 파일명", example = "business-registration.pdf")
            String originalFileName, // 원본 파일명
            @Schema(description = "MIME 유형", example = "application/pdf")
            String mimeType, // MIME 유형
            @Schema(description = "저장 경로", example = "1/BUSINESS_REGISTRATION/document.pdf")
            String storagePath, // 저장 경로
            @Schema(description = "문서 원문 Base64", example = "[MASKED]")
            String contentBase64, // 문서 원문 Base64
            @Schema(description = "파일 크기", example = "204800")
            Long fileSize // 파일 크기
    ) {
    }
}
