package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * KYC 신청 목록 검색 조건 DTO입니다.
 *
 * <p>관리자 KYC 신청 목록 조회에서 페이지, 신청 상태, 검색어, 제출일 범위,
 * AI 심사 상태, 보완요청 여부 조건을 표현합니다.</p>
 */
@Schema(description = "KYC 신청 목록 검색 조건")
public record AdminKycApplicationSearchRequest(
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
        String status,
        @Schema(description = "법인명, 사용자 이메일, 사업자등록번호 검색어", example = "kyvc")
        String keyword,
        @Schema(description = "제출일 시작일", example = "2026-05-01")
        LocalDate submittedFrom,
        @Schema(description = "제출일 종료일", example = "2026-05-31")
        LocalDate submittedTo,
        @Schema(description = "AI 심사 상태", example = "SUCCESS")
        String aiReviewStatus,
        @Schema(description = "보완요청 여부", example = "Y", allowableValues = {"Y", "N"})
        String supplementYn
) {

    /**
     * 요청 파라미터의 페이지 기본값과 범위를 보정하여 검색 조건을 생성합니다.
     *
     * @param page 요청 페이지 번호
     * @param size 요청 페이지 크기
     * @param status KYC 신청 상태
     * @param keyword 법인명, 사용자 이메일, 사업자등록번호 검색어
     * @param submittedFrom 제출일 시작일
     * @param submittedTo 제출일 종료일
     * @param aiReviewStatus AI 심사 상태
     * @param supplementYn 보완요청 여부
     * @return 보정된 KYC 신청 목록 검색 조건
     */
    public static AdminKycApplicationSearchRequest of(
            Integer page,
            Integer size,
            String status,
            String keyword,
            LocalDate submittedFrom,
            LocalDate submittedTo,
            String aiReviewStatus,
            String supplementYn
    ) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AdminKycApplicationSearchRequest(
                resolvedPage,
                resolvedSize,
                status,
                keyword,
                submittedFrom,
                submittedTo,
                aiReviewStatus,
                supplementYn
        );
    }
}
