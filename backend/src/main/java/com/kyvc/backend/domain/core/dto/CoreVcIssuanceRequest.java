package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VC 발급 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param credentialId Credential ID
 * @param kycId KYC 요청 ID
 * @param corporateId 법인 ID
 * @param issuerDid 발급자 DID
 * @param credentialType Credential 유형
 * @param callbackUrl Callback URL
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core VC 발급 요청")
public record CoreVcIssuanceRequest(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Credential ID", example = "11")
        Long credentialId, // Credential ID
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "발급자 DID", example = "did:web:issuer.kyvc.local")
        String issuerDid, // 발급자 DID
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "Callback URL", example = "http://localhost:8080/api/internal/core/callbacks/vc-issuance")
        String callbackUrl, // Callback URL
        @Schema(description = "요청 시각", example = "2026-05-06T10:10:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
