package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Credential Offer 응답
 *
 * @param offerId Offer ID
 * @param credentialId Credential ID
 * @param qrToken QR 토큰
 * @param expiresAt 만료 일시
 * @param qrPayload QR payload 데이터
 */
@Schema(description = "Credential Offer 응답")
public record CredentialOfferResponse(
        @Schema(description = "Offer ID", example = "1")
        Long offerId, // Offer ID
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "QR 토큰", example = "c89ad3f0-f3d1-4aef-bf9f-3ca21de66bd2")
        String qrToken, // QR 토큰
        @Schema(description = "만료 일시", example = "2026-05-07T23:59:59")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "QR payload 데이터")
        Map<String, Object> qrPayload // QR payload 데이터
) {
}

