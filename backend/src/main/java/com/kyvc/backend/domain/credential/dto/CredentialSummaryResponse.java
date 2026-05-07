package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential 요약 응답
 *
 * @param credentialId Credential ID
 * @param kycId KYC 신청 ID
 * @param credentialTypeCode Credential 유형 코드
 * @param credentialStatusCode Credential 상태 코드
 * @param issuerDid 발급자 DID
 * @param issuedAt 발급 일시
 * @param expiresAt 만료 일시
 * @param walletSaved Wallet 저장 여부
 * @param walletSavedAt Wallet 저장 일시
 */
@Schema(description = "Credential 요약 응답")
public record CredentialSummaryResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId, // KYC 신청 ID
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatusCode, // Credential 상태 코드
        @Schema(description = "발급자 DID", example = "did:kyvc:issuer")
        String issuerDid, // 발급자 DID
        @Schema(description = "발급 일시", example = "2026-05-07T12:30:00")
        LocalDateTime issuedAt, // 발급 일시
        @Schema(description = "만료 일시", example = "2027-05-07T12:30:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "Wallet 저장 여부", example = "true")
        boolean walletSaved, // Wallet 저장 여부
        @Schema(description = "Wallet 저장 일시", example = "2026-05-07T14:00:00")
        LocalDateTime walletSavedAt // Wallet 저장 일시
) {
}

