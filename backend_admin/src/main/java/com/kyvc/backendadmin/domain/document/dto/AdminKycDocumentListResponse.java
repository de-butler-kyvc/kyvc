package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 제출 문서 목록 응답 DTO입니다.
 *
 * <p>KYC 신청에 제출된 문서 목록을 관리자 화면에 전달하며, 파일 원본 경로나
 * 내부 저장 위치는 포함하지 않습니다.</p>
 */
@Schema(description = "KYC 제출 문서 목록 응답")
public record AdminKycDocumentListResponse(
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,
        @Schema(description = "제출 문서 목록")
        List<Item> documents
) {

    /**
     * KYC 제출 문서 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "KYC 제출 문서 목록 항목")
    public record Item(
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
            @Schema(description = "업로드 상태", example = "UPLOADED")
            String uploadStatus,
            @Schema(description = "업로드 시각")
            LocalDateTime uploadedAt
    ) {
    }
}
