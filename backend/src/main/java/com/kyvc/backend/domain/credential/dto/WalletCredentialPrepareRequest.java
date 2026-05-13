package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wallet Credential 준비 요청
 *
 * @param qrToken QR 토큰
 * @param deviceId 모바일 기기 ID
 * @param holderDid Holder DID
 * @param holderXrplAddress Holder XRPL 주소
 * @param holderKeyId Holder 키 ID
 * @param accepted 저장 동의 여부
 */
@Schema(description = "Wallet Credential 준비 요청")
public record WalletCredentialPrepareRequest(
        @Schema(description = "QR 토큰", example = "c89ad3f0-f3d1-4aef-bf9f-3ca21de66bd2")
        String qrToken, // QR 토큰
        @Schema(description = "모바일 기기 ID", example = "mobile-device-001")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "Holder DID", example = "did:xrpl:1:rHolder")
        String holderDid, // Holder DID
        @Schema(description = "Holder XRPL 주소", example = "rHolder")
        String holderXrplAddress, // Holder XRPL 주소
        @Schema(description = "Holder 키 ID", example = "did:xrpl:1:rHolder#holder-key-1")
        String holderKeyId, // Holder 키 ID
        @Schema(description = "저장 동의 여부", example = "true")
        Boolean accepted // 저장 동의 여부
) {
}
