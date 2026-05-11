package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 금융사 방문 KYC 목록 응답
 *
 * @param items KYC 목록
 * @param page 페이징 정보
 */
@Schema(description = "금융사 방문 KYC 목록 응답")
public record FinanceKycApplicationListResponse(
        @Schema(description = "KYC 목록")
        List<Item> items, // KYC 목록
        @Schema(description = "페이징 정보")
        PageInfo page // 페이징 정보
) {

    /**
     * 금융사 방문 KYC 목록 항목
     *
     * @param kycId KYC 신청 ID
     * @param status KYC 상태 코드
     * @param corporateId 법인 ID
     * @param corporateName 법인명
     * @param financeCustomerNo 금융사 고객번호
     * @param visitedAt 방문 일시
     */
    @Schema(description = "금융사 방문 KYC 목록 항목")
    public record Item(
            @Schema(description = "KYC 신청 ID", example = "1")
            Long kycId, // KYC 신청 ID
            @Schema(description = "KYC 상태 코드", example = "DRAFT")
            String status, // KYC 상태 코드
            @Schema(description = "법인 ID", example = "1")
            Long corporateId, // 법인 ID
            @Schema(description = "법인명", example = "주식회사 KYVC")
            String corporateName, // 법인명
            @Schema(description = "금융사 고객번호", example = "CUST-001")
            String financeCustomerNo, // 금융사 고객번호
            @Schema(description = "방문 일시", example = "2026-05-11T10:00:00")
            LocalDateTime visitedAt // 방문 일시
    ) {
    }

    /**
     * 페이징 정보
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 건수
     * @param totalPages 전체 페이지 수
     */
    @Schema(description = "페이징 정보")
    public record PageInfo(
            @Schema(description = "페이지 번호", example = "0")
            int page, // 페이지 번호
            @Schema(description = "페이지 크기", example = "20")
            int size, // 페이지 크기
            @Schema(description = "전체 건수", example = "1")
            long totalElements, // 전체 건수
            @Schema(description = "전체 페이지 수", example = "1")
            int totalPages // 전체 페이지 수
    ) {
    }
}
