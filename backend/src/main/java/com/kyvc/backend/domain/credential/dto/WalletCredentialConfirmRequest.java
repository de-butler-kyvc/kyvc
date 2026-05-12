package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wallet Credential 저장 확정 요청
 *
 * @param credentialId Credential ID
 * @param deviceId 모바일 기기 ID
 * @param walletSaved Wallet 저장 완료 여부
 * @param walletSavedAt Wallet 저장 완료 일시
 * @param credentialAcceptHash XRPL CredentialAccept 해시
 */
@Schema(description = "Wallet Credential 저장 확정 요청")
public record WalletCredentialConfirmRequest(
        @Schema(description = "Credential ID", example = "200")
        Long credentialId, // Credential ID
        @Schema(description = "모바일 기기 ID", example = "mobile-device-001")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "Wallet 저장 완료 여부", example = "true")
        Boolean walletSaved, // Wallet 저장 완료 여부
        @Schema(description = "Wallet 저장 완료 일시", example = "2026-05-12T16:35:00")
        LocalDateTime walletSavedAt, // Wallet 저장 완료 일시
        @Schema(description = "XRPL CredentialAccept 해시", example = "accept-hash")
        String credentialAcceptHash // XRPL CredentialAccept 해시
) {
}
