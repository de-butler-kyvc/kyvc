package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Credential 목록 검색 조건 DTO입니다.
 */
@Schema(description = "Credential 목록 검색 조건")
public record AdminCredentialSearchRequest(

        /** 페이지 번호 */
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** Credential 상태 코드 */
        @Schema(description = "Credential 상태 코드", example = "ISSUING")
        String status,

        /** 법인명 */
        @Schema(description = "법인명", example = "모의 재심사 법인")
        String corporateName,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "999-88-77777")
        String businessRegistrationNo,

        /** Issuer DID */
        @Schema(description = "Issuer DID", example = "did:kyvc:backend-admin")
        String issuerDid,

        /** 조회 시작일 */
        @Schema(description = "조회 시작일", example = "2026-05-01")
        LocalDate fromDate,

        /** 조회 종료일 */
        @Schema(description = "조회 종료일", example = "2026-05-31")
        LocalDate toDate
) {

    /**
     * 조회 조건의 페이지 값을 보정합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status Credential 상태 코드
     * @param corporateName 법인명
     * @param businessRegistrationNo 사업자등록번호
     * @param issuerDid Issuer DID
     * @param fromDate 조회 시작일
     * @param toDate 조회 종료일
     * @return 보정된 검색 조건
     */
    public static AdminCredentialSearchRequest of(
            Integer page,
            Integer size,
            String status,
            String corporateName,
            String businessRegistrationNo,
            String issuerDid,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AdminCredentialSearchRequest(
                normalizedPage,
                normalizedSize,
                status,
                corporateName,
                businessRegistrationNo,
                issuerDid,
                fromDate,
                toDate
        );
    }
}
