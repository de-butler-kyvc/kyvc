package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core Credential 상태 조회 응답
 *
 * @param issuerAccount Issuer XRPL Account
 * @param holderAccount Holder XRPL Account
 * @param credentialType Credential 유형
 * @param found Core 상태 엔트리 조회 여부
 * @param active Core active 여부
 * @param credentialStatusCode Backend 매핑 상태 코드
 * @param checkedAt 조회 시각
 * @param message 처리 메시지
 */
@Schema(description = "Core Credential 상태 조회 응답")
public record CoreCredentialStatusResponse(
        @Schema(description = "Issuer XRPL Account", example = "rIssuer")
        String issuerAccount, // Issuer XRPL Account
        @Schema(description = "Holder XRPL Account", example = "rHolder")
        String holderAccount, // Holder XRPL Account
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "Core 상태 엔트리 조회 여부", example = "true")
        boolean found, // Core 상태 엔트리 조회 여부
        @Schema(description = "Core active 여부", example = "true")
        boolean active, // Core active 여부
        @Schema(description = "Backend 매핑 상태 코드", example = "VALID")
        String credentialStatusCode, // Backend 매핑 상태 코드
        @Schema(description = "조회 시각", example = "2026-05-08T12:00:00")
        LocalDateTime checkedAt, // 조회 시각
        @Schema(description = "처리 메시지", example = "Core credential status synced.")
        String message // 처리 메시지
) {
}
