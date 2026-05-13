package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Credential Offer 생성 응답
 *
 * @param offerId Offer ID
 * @param kycId KYC 신청 ID
 * @param qrPayload QR payload 데이터
 * @param expiresAt 만료 일시
 * @param offerStatus Offer 상태 코드
 */
@Schema(description = "Credential Offer 생성 응답")
public record CredentialOfferCreateResponse(
        @Schema(description = "Offer ID", example = "100")
        Long offerId, // Offer ID
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId, // KYC 신청 ID
        @Schema(description = "QR payload 데이터")
        Map<String, Object> qrPayload, // QR payload 데이터
        @Schema(description = "만료 일시", example = "2026-05-12T16:30:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "Offer 상태 코드", example = "ACTIVE")
        String offerStatus // Offer 상태 코드
) {
}
