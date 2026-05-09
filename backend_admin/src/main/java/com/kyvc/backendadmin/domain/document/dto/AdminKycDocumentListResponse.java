package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 제출 문서 목록 응답 DTO입니다.
 *
 * <p>관리자 화면에 필요한 문서 메타데이터만 응답하며, 원본 파일 경로와 문서 해시는 응답하지 않습니다.</p>
 */
@Schema(description = "KYC 제출 문서 목록 응답")
public record AdminKycDocumentListResponse(
        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** 제출 문서 목록 */
        @Schema(description = "제출 문서 목록")
        List<Item> documents
) {

    /**
     * KYC 제출 문서 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "KYC 제출 문서 목록 항목")
    public record Item(
            /** 문서 ID */
            @Schema(description = "문서 ID", example = "200")
            Long documentId,

            /** 문서 유형 코드 */
            @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
            String documentType,

            /** 문서 유형명 */
            @Schema(description = "문서 유형명", example = "사업자등록증")
            String documentTypeName,

            /** 원본 파일명 */
            @Schema(description = "원본 파일명", example = "business-registration.pdf")
            String fileName,

            /** MIME 타입 */
            @Schema(description = "MIME 타입", example = "application/pdf")
            String mimeType,

            /** 파일 크기 */
            @Schema(description = "파일 크기(byte)", example = "123456")
            Long fileSize,

            /** 업로드 상태 */
            @Schema(description = "업로드 상태", example = "UPLOADED")
            String uploadStatus,

            /** 업로드 주체 유형 코드 */
            @Schema(description = "업로드 주체 유형 코드", example = "USER")
            String uploadedByType,

            /** 업로드 사용자 ID */
            @Schema(description = "업로드 사용자 ID", example = "1")
            Long uploadedByUserId,

            /** 업로드 시각 */
            @Schema(description = "업로드 시각")
            LocalDateTime uploadedAt
    ) {
    }
}
