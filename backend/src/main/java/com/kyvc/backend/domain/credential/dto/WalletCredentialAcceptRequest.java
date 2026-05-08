package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wallet Credential Offer 수락 요청
 *
 * @param qrToken QR 토큰
 * @param deviceId 기기 ID
 * @param holderDid Holder DID
 * @param holderXrplAddress Holder XRPL 주소
 * @param accepted 수락 여부
 */
@Schema(description = "Wallet Credential Offer 수락 요청")
public record WalletCredentialAcceptRequest(
        @Schema(description = "QR 토큰", example = "c89ad3f0-f3d1-4aef-bf9f-3ca21de66bd2")
        String qrToken, // QR 토큰
        @Schema(description = "기기 ID", example = "mobile-device-001")
        String deviceId, // 기기 ID
        @Schema(description = "Holder DID", example = "did:key:holder")
        String holderDid, // Holder DID
        @Schema(description = "Holder XRPL 주소", example = "rExampleAddress")
        String holderXrplAddress, // Holder XRPL 주소
        @Schema(description = "수락 여부", example = "true")
        Boolean accepted // 수락 여부
) {
}

