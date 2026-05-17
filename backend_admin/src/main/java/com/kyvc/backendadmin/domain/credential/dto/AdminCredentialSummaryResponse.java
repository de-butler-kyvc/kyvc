package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * VC 발급 상태 목록 조회 응답 DTO입니다.
 */
@Schema(description = "VC 발급 상태 목록 조회 응답")
public record AdminCredentialSummaryResponse(

        /** VC 발급 상태 목록 */
        @Schema(description = "VC 발급 상태 목록")
        List<Item> items,

        /** 현재 페이지 번호 */
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** 전체 건수 */
        @Schema(description = "전체 건수", example = "120")
        long totalElements,

        /** 전체 페이지 수 */
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages
) {

    /**
     * VC 발급 상태 목록 항목 DTO입니다.
     */
    @Schema(description = "VC 발급 상태 목록 항목")
    public record Item(

            /** Credential ID */
            @Schema(description = "Credential ID", example = "1")
            Long credentialId,

            /** KYC 신청 ID */
            @Schema(description = "KYC 신청 ID", example = "100")
            Long kycId,

            /** 법인 ID */
            @Schema(description = "법인 ID", example = "10")
            Long corporateId,

            /** 법인명 */
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName,

            /** 사업자등록번호 */
            @Schema(description = "사업자등록번호", example = "123-45-67890")
            String businessRegistrationNumber,

            /** Credential 유형 */
            @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
            String credentialType,

            /** Credential 상태 */
            @Schema(description = "Credential 상태", example = "ISSUING")
            String credentialStatus,

            /** Core 요청 상태 */
            @Schema(description = "Core 요청 상태", example = "QUEUED")
            String coreRequestStatus,

            /** XRPL 트랜잭션 해시 */
            @Schema(description = "XRPL 트랜잭션 해시")
            String xrplTxHash,

            /** 발급 시각 */
            @Schema(description = "발급 시각")
            LocalDateTime issuedAt,

            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {
    }

    /**
     * VC 발급 상태 목록 검색 조건 DTO입니다.
     */
    public record SearchRequest(
            int page,
            int size,
            String keyword,
            String credentialStatus,
            String coreRequestStatus,
            String corporateName,
            String businessRegistrationNumber,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        /**
         * 검색 조건을 생성합니다.
         *
         * @param page 페이지 번호
         * @param size 페이지 크기
         * @param keyword 검색어
         * @param credentialStatus Credential 상태
         * @param coreRequestStatus Core 요청 상태
         * @param corporateName 법인명
         * @param businessRegistrationNumber 사업자등록번호
         * @param fromDate 시작일
         * @param toDate 종료일
         * @return 검색 조건
         */
        public static SearchRequest of(
                Integer page,
                Integer size,
                String keyword,
                String credentialStatus,
                String coreRequestStatus,
                String corporateName,
                String businessRegistrationNumber,
                LocalDate fromDate,
                LocalDate toDate
        ) {
            int normalizedPage = page == null || page < 0 ? 0 : page;
            int normalizedSize = size == null || size < 1 ? 15 : Math.min(size, 100);
            return new SearchRequest(
                    normalizedPage,
                    normalizedSize,
                    keyword,
                    credentialStatus,
                    coreRequestStatus,
                    corporateName,
                    businessRegistrationNumber,
                    fromDate,
                    toDate
            );
        }
    }
}
