package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VC 발급 Callback 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param status Callback 처리 상태
 * @param credentialId Core Credential ID
 * @param credentialExternalId 외부 Credential ID
 * @param issuerDid Issuer DID
 * @param vcHash VC 해시
 * @param credentialHash Credential 해시
 * @param xrplTxHash XRPL 트랜잭션 해시
 * @param credentialStatusId Credential Status ID
 * @param errorMessage 실패 메시지
 * @param issuedAt 발급 완료 시각
 * @param expiresAt 만료 시각
 */
@Schema(description = "Core VC 발급 Callback 요청")
public record CoreVcIssuanceCallbackRequest(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Callback 처리 상태", example = "SUCCESS")
        String status, // Callback 처리 상태
        @Schema(description = "Core Credential ID", example = "urn:uuid:credential-001")
        String credentialId, // Core Credential ID
        @Schema(description = "외부 Credential ID", example = "urn:uuid:credential-001")
        String credentialExternalId, // 외부 Credential ID
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid, // Issuer DID
        @Schema(description = "VC 해시", example = "sha256:abc123")
        String vcHash, // VC 해시
        @Schema(description = "Credential 해시", example = "sha256:abc123")
        String credentialHash, // Credential 해시
        @Schema(description = "XRPL 트랜잭션 해시", example = "4A5B6C7D8E9F")
        String xrplTxHash, // XRPL 트랜잭션 해시
        @Schema(description = "Credential Status ID", example = "status-id")
        String credentialStatusId, // Credential Status ID
        @Schema(description = "실패 메시지", example = "VC issuance failed")
        String errorMessage, // 실패 메시지
        @Schema(description = "발급 완료 시각", example = "2026-05-06T15:00:00")
        LocalDateTime issuedAt, // 발급 완료 시각
        @Schema(description = "만료 시각", example = "2027-05-06T15:00:00")
        LocalDateTime expiresAt // 만료 시각
) {
}
