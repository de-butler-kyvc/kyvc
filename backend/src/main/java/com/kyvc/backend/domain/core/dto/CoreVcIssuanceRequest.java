package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core VC 발급 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param credentialId Credential ID
 * @param kycId KYC 신청 ID
 * @param corporateId 법인 ID
 * @param issuerAccount Issuer XRPL Account
 * @param issuerDid Issuer DID
 * @param issuerVerificationMethodId Issuer Verification Method ID
 * @param holderAccount Holder XRPL Account
 * @param holderDid Holder DID
 * @param credentialType Credential 유형
 * @param kycLevel KYC 레벨 코드
 * @param jurisdiction 관할 코드
 * @param claims VC Claim 데이터
 * @param validFrom VC 유효 시작 시각
 * @param validUntil VC 유효 종료 시각
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core VC 발급 요청")
public record CoreVcIssuanceRequest(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Credential ID", example = "11")
        Long credentialId, // Credential ID
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "Issuer XRPL Account", example = "rIssuer")
        String issuerAccount, // Issuer XRPL Account
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid, // Issuer DID
        @Schema(description = "Issuer Verification Method ID", example = "did:xrpl:1:rIssuer#issuer-key-1")
        String issuerVerificationMethodId, // Issuer Verification Method ID
        @Schema(description = "Holder XRPL Account", example = "rHolder")
        String holderAccount, // Holder XRPL Account
        @Schema(description = "Holder DID", example = "did:xrpl:1:rHolder")
        String holderDid, // Holder DID
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "KYC 레벨 코드", example = "BASIC")
        String kycLevel, // KYC 레벨 코드
        @Schema(description = "관할 코드", example = "KR")
        String jurisdiction, // 관할 코드
        @Schema(description = "VC Claim 데이터")
        Map<String, Object> claims, // VC Claim 데이터
        @Schema(description = "VC 유효 시작 시각", example = "2026-05-07T12:00:00")
        LocalDateTime validFrom, // VC 유효 시작 시각
        @Schema(description = "VC 유효 종료 시각", example = "2027-05-07T12:00:00")
        LocalDateTime validUntil, // VC 유효 종료 시각
        @Schema(description = "요청 시각", example = "2026-05-06T10:10:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
