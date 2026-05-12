package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wallet Credential Offer 응답
 *
 * @param offerId Offer ID
 * @param kycId KYC 신청 ID
 * @param credentialId Credential ID
 * @param credentialTypeCode Credential 유형 코드
 * @param issuerDid 발급자 DID
 * @param corporateName 법인명
 * @param businessNumber 사업자등록번호
 * @param expiresAt 만료 일시
 * @param alreadySaved Wallet 저장 여부
 */
@Schema(description = "Wallet Credential Offer 응답")
public record WalletCredentialOfferResponse(
        @Schema(description = "Offer ID", example = "1")
        Long offerId, // Offer ID
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId, // KYC 신청 ID
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "발급자 DID", example = "did:kyvc:issuer")
        String issuerDid, // 발급자 DID
        @Schema(description = "법인명", example = "KYVC")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessNumber, // 사업자등록번호
        @Schema(description = "만료 일시", example = "2026-05-07T23:59:59")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "Wallet 저장 여부", example = "false")
        boolean alreadySaved // Wallet 저장 여부
) {
}

