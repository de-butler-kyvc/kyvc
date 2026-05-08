package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Wallet Credential Offer 수락 응답
 *
 * @param credentialId Credential ID
 * @param walletSaved Wallet 저장 여부
 * @param walletSavedAt Wallet 저장 일시
 * @param credentialPayload Wallet 표시용 Credential payload
 * @param message 처리 메시지
 */
@Schema(description = "Wallet Credential Offer 수락 응답")
public record WalletCredentialAcceptResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Wallet 저장 여부", example = "true")
        boolean walletSaved, // Wallet 저장 여부
        @Schema(description = "Wallet 저장 일시", example = "2026-05-07T14:00:00")
        LocalDateTime walletSavedAt, // Wallet 저장 일시
        @Schema(description = "Wallet 표시용 Credential payload")
        Map<String, Object> credentialPayload, // Wallet 표시용 Credential payload
        @Schema(description = "처리 메시지", example = "Credential이 Wallet에 저장되었습니다.")
        String message // 처리 메시지
) {
}

