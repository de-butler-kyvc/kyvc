package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Credential 목록 조회 응답 DTO입니다.
 */
@Schema(description = "Credential 목록 조회 응답")
public record AdminCredentialListResponse(

        /** Credential 목록 */
        @Schema(description = "Credential 목록")
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
     * Credential 목록 항목 DTO입니다.
     */
    @Schema(description = "Credential 목록 항목")
    public record Item(

            /** Credential ID */
            @Schema(description = "Credential ID", example = "1")
            Long credentialId,

            /** 외부 Credential ID */
            @Schema(description = "외부 Credential ID", example = "kyvc-credential-550e8400-e29b-41d4-a716-446655440000")
            String credentialExternalId,

            /** 법인 ID */
            @Schema(description = "법인 ID", example = "10")
            Long corporateId,

            /** 법인명 */
            @Schema(description = "법인명", example = "모의 재심사 법인")
            String corporateName,

            /** 사업자등록번호 */
            @Schema(description = "사업자등록번호", example = "999-88-77777")
            String businessRegistrationNo,

            /** Credential 유형 코드 */
            @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
            String credentialTypeCode,

            /** Credential 상태 코드 */
            @Schema(description = "Credential 상태 코드", example = "ISSUING")
            String credentialStatusCode,

            /** Issuer DID */
            @Schema(description = "Issuer DID", example = "did:kyvc:backend-admin")
            String issuerDid,

            /** XRPL 트랜잭션 해시 */
            @Schema(description = "XRPL 트랜잭션 해시")
            String xrplTxHash,

            /** 발급 시각 */
            @Schema(description = "발급 시각")
            LocalDateTime issuedAt,

            /** 만료 시각 */
            @Schema(description = "만료 시각")
            LocalDateTime expiresAt,

            /** Wallet 저장 여부 */
            @Schema(description = "Wallet 저장 여부", example = "N")
            String walletSavedYn
    ) {
    }
}
