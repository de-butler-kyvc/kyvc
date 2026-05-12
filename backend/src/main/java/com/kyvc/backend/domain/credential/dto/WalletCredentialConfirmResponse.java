package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Wallet Credential 저장 확정 응답
 *
 * @param offerId Offer ID
 * @param credentialId Credential ID
 * @param walletSaved Wallet 저장 여부
 * @param offerStatus Offer 상태 코드
 * @param credentialStatus Credential 상태 코드
 * @param walletSavedAt Wallet 저장 완료 일시
 */
@Schema(description = "Wallet Credential 저장 확정 응답")
public record WalletCredentialConfirmResponse(
        @Schema(description = "Offer ID", example = "100")
        Long offerId, // Offer ID
        @Schema(description = "Credential ID", example = "200")
        Long credentialId, // Credential ID
        @Schema(description = "Wallet 저장 여부", example = "true")
        boolean walletSaved, // Wallet 저장 여부
        @Schema(description = "Offer 상태 코드", example = "USED")
        String offerStatus, // Offer 상태 코드
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatus, // Credential 상태 코드
        @Schema(description = "Wallet 저장 완료 일시", example = "2026-05-12T16:35:00")
        LocalDateTime walletSavedAt // Wallet 저장 완료 일시
) {
}
