package com.kyvc.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 보완요청 문서 응답
 *
 * @param supplementDocumentId 보완요청 문서 매핑 ID
 * @param documentId 문서 ID
 * @param documentTypeCode 문서 유형 코드
 * @param fileName 원본 파일명
 * @param mimeType MIME 타입
 * @param fileSize 파일 크기
 * @param documentHash 문서 해시
 * @param uploadedAt 업로드 일시
 */
@Schema(description = "보완요청 문서 응답")
public record SupplementDocumentResponse(
        @Schema(description = "보완요청 문서 매핑 ID", example = "1")
        Long supplementDocumentId, // 보완요청 문서 매핑 ID
        @Schema(description = "문서 ID", example = "10")
        Long documentId, // 문서 ID
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode, // 문서 유형 코드
        @Schema(description = "원본 파일명", example = "business-registration.pdf")
        String fileName, // 원본 파일명
        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType, // MIME 타입
        @Schema(description = "파일 크기", example = "1024")
        Long fileSize, // 파일 크기
        @Schema(description = "문서 해시", example = "e3b0c44298fc1c149afbf4c8996fb924")
        String documentHash, // 문서 해시
        @Schema(description = "업로드 일시", example = "2026-05-05T10:30:00")
        LocalDateTime uploadedAt // 업로드 일시
) {
}
