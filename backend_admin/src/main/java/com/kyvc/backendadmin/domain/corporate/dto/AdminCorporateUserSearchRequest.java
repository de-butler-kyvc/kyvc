package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 목록 검색 조건 DTO입니다.
 *
 * <p>관리자 법인 사용자 목록 조회에서 페이지, 검색어, 사용자 상태, 법인명,
 * 최근 KYC 상태 조건을 표현합니다.</p>
 */
@Schema(description = "법인 사용자 목록 검색 조건")
public record AdminCorporateUserSearchRequest(
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "사용자 이메일, 법인명, 사업자등록번호 검색어", example = "kyvc")
        String keyword,
        @Schema(description = "사용자 상태", example = "ACTIVE")
        String status,
        @Schema(description = "법인명 검색어", example = "케이와이브이씨")
        String corporateName,
        @Schema(description = "최근 KYC 상태", example = "APPROVED")
        String kycStatus
) {

    /**
     * 요청 파라미터의 페이지 기본값과 범위를 보정하여 검색 조건을 생성합니다.
     *
     * @param page 요청 페이지 번호
     * @param size 요청 페이지 크기
     * @param keyword 사용자 이메일, 법인명, 사업자등록번호 검색어
     * @param status 사용자 상태
     * @param corporateName 법인명 검색어
     * @param kycStatus 최근 KYC 상태
     * @return 보정된 법인 사용자 검색 조건
     */
    public static AdminCorporateUserSearchRequest of(
            Integer page,
            Integer size,
            String keyword,
            String status,
            String corporateName,
            String kycStatus
    ) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? 15 : Math.min(size, 100);
        return new AdminCorporateUserSearchRequest(
                resolvedPage,
                resolvedSize,
                keyword,
                status,
                corporateName,
                kycStatus
        );
    }
}
