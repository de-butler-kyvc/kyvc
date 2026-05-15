package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Wallet Credential 준비 응답
 *
 * @param offerId Offer ID
 * @param credentialId Credential ID
 * @param prepared 준비 완료 여부
 * @param credentialPayload Wallet 저장용 Credential payload
 * @param documentAttachments Android 저장용 KYC 심사 문서 첨부 목록
 * @param documentAttachmentManifest VP 제출용 문서 첨부 manifest
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
        Map<String, Object> credentialPayload, // Wallet 저장용 Credential payload
        @Schema(description = "Android 저장용 KYC 심사 문서 첨부 목록")
        List<Map<String, Object>> documentAttachments, // Android 저장용 KYC 심사 문서 첨부 목록
        @Schema(description = "VP 제출용 문서 첨부 manifest")
        Map<String, Object> documentAttachmentManifest // VP 제출용 문서 첨부 manifest
) {
    public WalletCredentialPrepareResponse(
            Long offerId, // Offer ID
            Long credentialId, // Credential ID
            boolean prepared, // 준비 완료 여부
            Map<String, Object> credentialPayload // Wallet 저장용 Credential payload
    ) {
        this(offerId, credentialId, prepared, credentialPayload, List.of(), Map.of("attachments", List.of()));
    }
}
