package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wallet Credential 상세 응답
 *
 * @param credentialId Credential ID
 * @param credentialExternalId 외부 Credential ID
 * @param credentialTypeCode Credential 유형 코드
 * @param credentialStatusCode Credential 상태 코드
 * @param issuerDid 발급자 DID
 * @param vcHash VC 해시
 * @param xrplTxHash XRPL 트랜잭션 해시
 * @param issuedAt 발급 일시
 * @param expiresAt 만료 일시
 * @param walletSavedAt Wallet 저장 일시
 * @param holderDid Holder DID
 * @param holderXrplAddress Holder XRPL 주소
 * @param credentialStatusId Credential 상태 ID
 * @param credentialStatusPurposeCode Credential 상태 목적 코드
 * @param kycLevelCode KYC 레벨 코드
 * @param jurisdictionCode 관할 코드
 */
@Schema(description = "Wallet Credential 상세 응답")
public record WalletCredentialDetailResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "외부 Credential ID", example = "cred-ext-001")
        String credentialExternalId, // 외부 Credential ID
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatusCode, // Credential 상태 코드
        @Schema(description = "발급자 DID", example = "did:kyvc:issuer")
        String issuerDid, // 발급자 DID
        @Schema(description = "VC 해시", example = "vc-hash")
        String vcHash, // VC 해시
        @Schema(description = "XRPL 트랜잭션 해시", example = "tx-hash")
        String xrplTxHash, // XRPL 트랜잭션 해시
        @Schema(description = "발급 일시", example = "2026-05-07T12:30:00")
        LocalDateTime issuedAt, // 발급 일시
        @Schema(description = "만료 일시", example = "2027-05-07T12:30:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "Wallet 저장 일시", example = "2026-05-07T14:00:00")
        LocalDateTime walletSavedAt, // Wallet 저장 일시
        @Schema(description = "Holder DID", example = "did:key:holder")
        String holderDid, // Holder DID
        @Schema(description = "Holder XRPL 주소", example = "rExampleAddress")
        String holderXrplAddress, // Holder XRPL 주소
        @Schema(description = "Credential 상태 ID", example = "status-001")
        String credentialStatusId, // Credential 상태 ID
        @Schema(description = "Credential 상태 목적 코드", example = "revocation")
        String credentialStatusPurposeCode, // Credential 상태 목적 코드
        @Schema(description = "KYC 레벨 코드", example = "STANDARD")
        String kycLevelCode, // KYC 레벨 코드
        @Schema(description = "관할 코드", example = "KR")
        String jurisdictionCode // 관할 코드
) {
}

