package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 문서 목록 페이지 응답
 *
 * @param items 문서 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "사용자 문서 목록 페이지 응답")
public record UserDocumentListResponse(
        @Schema(description = "문서 목록")
        List<Item> items, // 문서 목록
        @Schema(description = "현재 페이지 번호", example = "0")
        int page, // 현재 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 건수", example = "100")
        long totalElements, // 전체 건수
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages // 전체 페이지 수
) {

    public UserDocumentListResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * 사용자 문서 목록 항목
     *
     * @param documentId 문서 ID
     * @param kycId KYC 신청 ID
     * @param corporateId 법인 ID
     * @param corporateName 법인명
     * @param documentTypeCode 문서 유형 코드
     * @param documentTypeName 문서 유형명
     * @param fileName 원본 파일명
     * @param mimeType MIME 타입
     * @param fileSize 파일 크기
     * @param uploadStatusCode 업로드 상태 코드
     * @param uploadedAt 업로드 일시
     * @param kycStatusCode KYC 상태 코드
     * @param deleteRequestId 최근 삭제 요청 ID
     * @param deleteRequestStatusCode 최근 삭제 요청 상태 코드
     */
    @Schema(description = "사용자 문서 목록 항목")
    public record Item(
            @Schema(description = "문서 ID", example = "1")
            Long documentId, // 문서 ID
            @Schema(description = "KYC 신청 ID", example = "10")
            Long kycId, // KYC 신청 ID
            @Schema(description = "법인 ID", example = "20")
            Long corporateId, // 법인 ID
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName, // 법인명
            @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
            String documentTypeCode, // 문서 유형 코드
            @Schema(description = "문서 유형명", example = "사업자등록증")
            String documentTypeName, // 문서 유형명
            @Schema(description = "원본 파일명", example = "business-registration.pdf")
            String fileName, // 원본 파일명
            @Schema(description = "MIME 타입", example = "application/pdf")
            String mimeType, // MIME 타입
            @Schema(description = "파일 크기", example = "123456")
            Long fileSize, // 파일 크기
            @Schema(description = "업로드 상태 코드", example = "UPLOADED")
            String uploadStatusCode, // 업로드 상태 코드
            @Schema(description = "업로드 일시", example = "2026-05-11T10:30:00")
            LocalDateTime uploadedAt, // 업로드 일시
            @Schema(description = "KYC 상태 코드", example = "SUBMITTED")
            String kycStatusCode, // KYC 상태 코드
            @Schema(description = "최근 삭제 요청 ID", example = "5", nullable = true)
            Long deleteRequestId, // 최근 삭제 요청 ID
            @Schema(description = "최근 삭제 요청 상태 코드", example = "REQUESTED", nullable = true)
            String deleteRequestStatusCode // 최근 삭제 요청 상태 코드
    ) {
    }
}
