package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 신청 목록 응답 DTO입니다.
 *
 * <p>검색된 KYC 신청 목록과 기존 프로젝트의 페이지 응답 형식에 맞춘 페이지 정보를 전달합니다.</p>
 */
@Schema(description = "KYC 신청 목록 응답")
public record AdminKycApplicationListResponse(
        @Schema(description = "KYC 신청 목록")
        List<Item> items,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "120")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages
) {

    /**
     * KYC 신청 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "KYC 신청 목록 항목")
    public record Item(
            @Schema(description = "KYC 신청 ID", example = "100")
            Long kycId,
            @Schema(description = "법인 ID", example = "10")
            Long corporateId,
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName,
            @Schema(description = "사업자등록번호", example = "123-45-67890")
            String businessRegistrationNo,
            @Schema(description = "법인 유형 코드", example = "CORPORATION")
            String corporateTypeCode,
            @Schema(description = "법인 유형명", example = "주식회사")
            String corporateTypeName,
            @Schema(description = "법인등록번호", example = "110111-1234567")
            String corporateRegistrationNo,
            @Schema(description = "설립일", example = "2020-01-01")
            LocalDate establishedDate,
            @Schema(description = "신청 사용자 ID", example = "1")
            Long applicantUserId,
            @Schema(description = "신청 사용자 이메일", example = "corp@kyvc.local")
            String applicantEmail,
            @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
            String kycStatus,
            @Schema(description = "AI 심사 상태", example = "SUCCESS")
            String aiReviewStatus,
            @Schema(description = "AI 심사 결과", example = "PASS")
            String aiReviewResult,
            @Schema(description = "AI 신뢰도 점수", example = "92.50")
            BigDecimal aiConfidenceScore,
            @Schema(description = "보완요청 여부", example = "Y")
            String supplementYn,
            @Schema(description = "최근 보완요청 상태", example = "REQUESTED")
            String latestSupplementStatus,
            @Schema(description = "제출 시각")
            LocalDateTime submittedAt,
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }
}
