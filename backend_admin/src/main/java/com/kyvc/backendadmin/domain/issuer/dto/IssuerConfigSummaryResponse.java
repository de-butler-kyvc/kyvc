package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Issuer 발급 설정 목록 응답 DTO입니다. */
@Schema(description = "Issuer 발급 설정 목록 응답")
public record IssuerConfigSummaryResponse(
        /** Issuer 발급 설정 목록 */
        @Schema(description = "Issuer 발급 설정 목록")
        List<Item> items,
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
    /** Issuer 발급 설정 목록 항목 DTO입니다. */
    @Schema(description = "Issuer 발급 설정 목록 항목")
    public record Item(
            /** Issuer 발급 설정 ID */
            @Schema(description = "Issuer 발급 설정 ID", example = "1")
            Long issuerConfigId,
            /** Issuer DID */
            @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
            String issuerDid,
            /** Issuer 이름 */
            @Schema(description = "Issuer 이름", example = "KYvC Platform Issuer")
            String issuerName,
            /** Issuer 유형 */
            @Schema(description = "Issuer 유형", example = "PLATFORM")
            String issuerType,
            /** Credential 유형 */
            @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
            String credentialType,
            /** 기본 Issuer 여부 */
            @Schema(description = "기본 Issuer 여부(Y/N)", example = "Y")
            String defaultYn,
            /** 설정 상태 */
            @Schema(description = "설정 상태", example = "ACTIVE")
            String status,
            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            /** 수정 시각 */
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }

    /** Issuer 발급 설정 검색 조건 DTO입니다. */
    public record SearchRequest(
            int page,
            int size,
            String keyword,
            String status,
            String issuerType,
            String credentialType
    ) {
        /** 검색 조건을 생성합니다. */
        public static SearchRequest of(Integer page, Integer size, String keyword, String status, String issuerType, String credentialType) {
            return new SearchRequest(
                    page == null || page < 0 ? 0 : page,
                    size == null || size < 1 ? 15 : Math.min(size, 100),
                    keyword,
                    status,
                    issuerType,
                    credentialType
            );
        }
    }
}
