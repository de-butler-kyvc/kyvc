package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Wallet Credential 준비 응답
 *
 * @param offerId Offer ID
 * @param credentialId Credential ID
 * @param prepared 준비 완료 여부
 * @param credentialPayload Wallet 저장용 Credential payload
 */
@Schema(description = "Wallet Credential 준비 응답")
public record WalletCredentialPrepareResponse(
        @Schema(description = "Offer ID", example = "100")
        Long offerId, // Offer ID
        @Schema(description = "Credential ID", example = "200")
        Long credentialId, // Credential ID
        @Schema(description = "준비 완료 여부", example = "true")
        boolean prepared, // 준비 완료 여부
        @Schema(description = "Wallet 저장용 Credential payload")
        Map<String, Object> credentialPayload // Wallet 저장용 Credential payload
) {
}
