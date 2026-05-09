package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 제출 문서 미리보기 응답 DTO입니다.
 *
 * <p>관리자 문서 미리보기에 필요한 임시 URL과 만료 시각, 파일 메타데이터를 전달하며
 * 원본 파일 경로는 포함하지 않습니다.</p>
 */
@Schema(description = "KYC 제출 문서 미리보기 응답")
public record AdminKycDocumentPreviewResponse(
        @Schema(description = "문서 ID", example = "200")
        Long documentId,
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentType,
        @Schema(description = "문서 유형명", example = "사업자등록증")
        String documentTypeName,
        @Schema(description = "원본 파일명", example = "business-registration.pdf")
        String fileName,
        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType,
        @Schema(description = "파일 크기(byte)", example = "123456")
        Long fileSize,
        @Schema(description = "업로드 주체 유형 코드", example = "USER")
        String uploadedByTypeCode,
        @Schema(description = "업로드 사용자 ID", example = "1")
        Long uploadedByUserId,
        @Schema(description = "업로드 사용자 이름", example = "홍길동")
        String uploadedByUserName,
        @Schema(description = "미리보기 URL. 원본 파일 경로가 아닌 만료 시간이 있는 임시 접근 URL입니다.")
        String previewUrl,
        @Schema(description = "미리보기 URL 만료 시각")
        LocalDateTime expiresAt
) {
}
