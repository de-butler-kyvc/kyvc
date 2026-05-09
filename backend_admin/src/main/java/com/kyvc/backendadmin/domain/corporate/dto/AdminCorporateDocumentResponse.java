package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 법인문서 조회 응답 DTO입니다.
 */
@Schema(description = "법인문서 조회 응답")
public record AdminCorporateDocumentResponse(
        @Schema(description = "법인문서 ID", example = "100")
        Long corporateDocumentId,
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode,
        @Schema(description = "문서 유형명", example = "사업자등록증")
        String documentTypeName,
        @Schema(description = "파일명", example = "business-registration.pdf")
        String fileName,
        @Schema(description = "파일 경로. 원본 저장 경로 노출 정책에 따라 null로 응답할 수 있습니다.")
        String filePath,
        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType,
        @Schema(description = "파일 크기", example = "123456")
        Long fileSize,
        @Schema(description = "문서 해시")
        String documentHash,
        @Schema(description = "업로드 상태 코드", example = "UPLOADED")
        String uploadStatusCode,
        @Schema(description = "업로드 주체 코드", example = "USER")
        String uploadedByTypeCode,
        @Schema(description = "업로드 사용자 ID", example = "1")
        Long uploadedByUserId,
        @Schema(description = "업로드 사용자명", example = "홍길동")
        String uploadedByUserName,
        @Schema(description = "업로드 일시")
        LocalDateTime uploadedAt,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
}
