package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core Credential 폐기 요청
 *
 * @param issuerAccount Issuer XRPL Account
 * @param issuerSeed Issuer seed
 * @param holderAccount Holder XRPL Account
 * @param credentialType Credential 유형
 * @param credentialStatusId Credential Status ID
 * @param credentialExternalId Core 외부 Credential ID
 * @param holderDid Holder DID
 * @param issuerDid Issuer DID
 * @param vct Verifiable Credential Type
 * @param xrplJsonRpcUrl XRPL JSON RPC URL
 * @param allowMainnet mainnet 허용 여부
 * @param statusMode 상태 저장 모드
 * @param reason 폐기 사유
 */
@Schema(description = "Core Credential 폐기 요청")
public record CoreRevokeCredentialRequest(
        @Schema(description = "Issuer XRPL Account", example = "rIssuer")
        String issuerAccount, // Issuer XRPL Account
        @Schema(description = "Issuer seed")
        String issuerSeed, // Issuer seed
        @Schema(description = "Holder XRPL Account", example = "rHolder")
        String holderAccount, // Holder XRPL Account
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "Credential Status ID", example = "status-id")
        String credentialStatusId, // Credential Status ID
        @Schema(description = "Core 외부 Credential ID", example = "cred-001")
        String credentialExternalId, // Core 외부 Credential ID
        @Schema(description = "Holder DID", example = "did:xrpl:1:rHolder")
        String holderDid, // Holder DID
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid, // Issuer DID
        @Schema(description = "Verifiable Credential Type", example = "https://kyvc.example/credentials/legal-entity-kyc")
        String vct, // Verifiable Credential Type
        @Schema(description = "XRPL JSON RPC URL")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @Schema(description = "mainnet 허용 여부", example = "false")
        Boolean allowMainnet, // mainnet 허용 여부
        @Schema(description = "상태 저장 모드", example = "xrpl")
        String statusMode, // 상태 저장 모드
        @Schema(description = "폐기 사유", example = "User requested revocation")
        String reason // 폐기 사유
) {
}
