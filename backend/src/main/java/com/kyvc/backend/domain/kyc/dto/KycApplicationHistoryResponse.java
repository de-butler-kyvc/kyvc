package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 신청 이력 페이지 응답
 *
 * @param items KYC 신청 이력 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "KYC 신청 이력 페이지 응답")
public record KycApplicationHistoryResponse(
        @Schema(description = "KYC 신청 이력 목록")
        List<Item> items, // KYC 신청 이력 목록
        @Schema(description = "현재 페이지 번호", example = "0")
        int page, // 현재 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 건수", example = "100")
        long totalElements, // 전체 건수
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages // 전체 페이지 수
) {

    public KycApplicationHistoryResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * KYC 신청 이력 항목
     *
     * @param kycId KYC 신청 ID
     * @param corporateId 법인 ID
     * @param corporateName 법인명
     * @param businessRegistrationNo 사업자등록번호
     * @param corporateTypeCode 법인 유형 코드
     * @param kycStatusCode KYC 상태 코드
     * @param aiReviewStatusCode AI 심사 상태 코드
     * @param aiReviewResultCode AI 심사 결과 코드
     * @param submittedAt 제출 일시
     * @param approvedAt 승인 일시
     * @param rejectedAt 반려 일시
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     * @param credentialId Credential ID
     * @param credentialStatusCode Credential 상태 코드
     * @param credentialIssuedAt Credential 발급 일시
     */
    @Schema(description = "KYC 신청 이력 항목")
    public record Item(
            @Schema(description = "KYC 신청 ID", example = "1")
            Long kycId, // KYC 신청 ID
            @Schema(description = "법인 ID", example = "10")
            Long corporateId, // 법인 ID
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName, // 법인명
            @Schema(description = "사업자등록번호", example = "123-45-67890")
            String businessRegistrationNo, // 사업자등록번호
            @Schema(description = "법인 유형 코드", example = "CORPORATION")
            String corporateTypeCode, // 법인 유형 코드
            @Schema(description = "KYC 상태 코드", example = "APPROVED")
            String kycStatusCode, // KYC 상태 코드
            @Schema(description = "AI 심사 상태 코드", example = "SUCCESS", nullable = true)
            String aiReviewStatusCode, // AI 심사 상태 코드
            @Schema(description = "AI 심사 결과 코드", example = "PASS", nullable = true)
            String aiReviewResultCode, // AI 심사 결과 코드
            @Schema(description = "제출 일시", example = "2026-05-11T10:30:00")
            LocalDateTime submittedAt, // 제출 일시
            @Schema(description = "승인 일시", example = "2026-05-12T10:30:00")
            LocalDateTime approvedAt, // 승인 일시
            @Schema(description = "반려 일시", example = "2026-05-12T10:30:00")
            LocalDateTime rejectedAt, // 반려 일시
            @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
            LocalDateTime createdAt, // 생성 일시
            @Schema(description = "수정 일시", example = "2026-05-11T11:00:00")
            LocalDateTime updatedAt, // 수정 일시
            @Schema(description = "Credential ID", example = "100", nullable = true)
            Long credentialId, // Credential ID
            @Schema(description = "Credential 상태 코드", example = "VALID", nullable = true)
            String credentialStatusCode, // Credential 상태 코드
            @Schema(description = "Credential 발급 일시", example = "2026-05-12T11:00:00", nullable = true)
            LocalDateTime credentialIssuedAt // Credential 발급 일시
    ) {
    }
}
