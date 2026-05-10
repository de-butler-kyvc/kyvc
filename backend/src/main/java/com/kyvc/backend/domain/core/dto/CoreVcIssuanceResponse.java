package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VC 발급 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 * @param credentialExternalId Core 외부 Credential ID
 * @param credentialType Core Credential 유형
 * @param issuerDid Issuer DID
 * @param format Core VC format
 * @param credentialPayloadJson Core credential JSON 원문
 * @param credentialJwt Core credential JWT 원문
 * @param vcHash VC 해시
 * @param xrplTxHash XRPL 트랜잭션 해시
 * @param credentialStatusId Credential Status ID
 * @param issuedAt 발급 시각
 * @param expiresAt 만료 시각
 */
@Schema(description = "Core VC 발급 요청 응답")
public record CoreVcIssuanceResponse(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "VALID")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VC issuance request accepted by core.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:10:00")
        LocalDateTime requestedAt, // 요청 시각
        @Schema(description = "Core 외부 Credential ID", example = "cred-001")
        String credentialExternalId, // Core 외부 Credential ID
        @Schema(description = "Core Credential 유형", example = "56435F5354415455535F56313A...")
        String credentialType, // Core Credential 유형
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid, // Issuer DID
        @Schema(description = "Core VC format", example = "vc+jwt")
        String format, // Core VC format
        @Schema(description = "Core credential JSON 원문")
        String credentialPayloadJson, // Core credential JSON 원문
        @Schema(description = "Core credential JWT 원문", example = "eyJ...")
        String credentialJwt, // Core credential JWT 원문
        @Schema(description = "VC 해시", example = "vc-hash-value")
        String vcHash, // VC 해시
        @Schema(description = "XRPL 트랜잭션 해시", example = "tx-hash-value")
        String xrplTxHash, // XRPL 트랜잭션 해시
        @Schema(description = "Credential Status ID", example = "status-id")
        String credentialStatusId, // Credential Status ID
        @Schema(description = "발급 시각", example = "2026-05-07T12:00:00")
        LocalDateTime issuedAt, // 발급 시각
        @Schema(description = "만료 시각", example = "2027-05-07T12:00:00")
        LocalDateTime expiresAt // 만료 시각
) {
}
