package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Core Credential 폐기 요청
 *
 * @param issuerAccount Issuer XRPL Account
 * @param holderAccount Holder XRPL Account
 * @param credentialType Credential 유형
 * @param credentialStatusId Credential Status ID
 * @param credentialExternalId Core 외부 Credential ID
 * @param reason 폐기 사유
 */
@Schema(description = "Core Credential 폐기 요청")
public record CoreRevokeCredentialRequest(
        @Schema(description = "Issuer XRPL Account", example = "rIssuer")
        String issuerAccount, // Issuer XRPL Account
        @Schema(description = "Holder XRPL Account", example = "rHolder")
        String holderAccount, // Holder XRPL Account
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "Credential Status ID", example = "status-id")
        String credentialStatusId, // Credential Status ID
        @Schema(description = "Core 외부 Credential ID", example = "cred-001")
        String credentialExternalId, // Core 외부 Credential ID
        @Schema(description = "폐기 사유", example = "User requested revocation")
        String reason // 폐기 사유
) {
}
