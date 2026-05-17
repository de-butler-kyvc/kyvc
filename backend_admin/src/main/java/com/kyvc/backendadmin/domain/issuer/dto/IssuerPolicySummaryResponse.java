package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Issuer 정책 목록 응답 DTO입니다. */
@Schema(description = "Issuer 정책 목록 응답")
public record IssuerPolicySummaryResponse(
        /** Issuer 정책 목록 */
        @Schema(description = "Issuer 정책 목록")
        List<IssuerPolicyResponse> items,
        /** 현재 페이지 번호 */
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,
        /** 전체 건수 */
        @Schema(description = "전체 건수", example = "100")
        long totalElements,
        /** 전체 페이지 수 */
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
    /** Issuer 정책 검색 조건 DTO입니다. */
    public record SearchRequest(
            int page,
            int size,
            String keyword,
            String policyType,
            String status,
            String issuerDid,
            String issuerName,
            String credentialType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        /** 검색 조건을 생성합니다. */
        public static SearchRequest of(Integer page, Integer size, String keyword, String policyType, String status,
                                       String issuerDid, String issuerName, String credentialType,
                                       LocalDate startDate, LocalDate endDate) {
            return new SearchRequest(
                    page == null || page < 0 ? 0 : page,
                    size == null || size < 1 ? 15 : Math.min(size, 100),
                    keyword,
                    policyType,
                    status,
                    issuerDid,
                    issuerName,
                    credentialType,
                    startDate,
                    endDate
            );
        }
    }
}
