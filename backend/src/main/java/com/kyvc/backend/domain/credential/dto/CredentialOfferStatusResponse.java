package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Credential Offer 상태 응답
 *
 * @param offerId Offer ID
 * @param kycId KYC 신청 ID
 * @param offerStatus Offer 상태 코드
 * @param credentialId Credential ID
 * @param credentialStatus Credential 상태 코드
 * @param walletSaved Wallet 저장 여부
 * @param usedAt 사용 완료 일시
 * @param expiresAt 만료 일시
 */
@Schema(description = "Credential Offer 상태 응답")
public record CredentialOfferStatusResponse(
        @Schema(description = "Offer ID", example = "100")
        Long offerId, // Offer ID
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId, // KYC 신청 ID
        @Schema(description = "Offer 상태 코드", example = "ACTIVE")
        String offerStatus, // Offer 상태 코드
        @Schema(description = "Credential ID", example = "200")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatus, // Credential 상태 코드
        @Schema(description = "Wallet 저장 여부", example = "false")
        Boolean walletSaved, // Wallet 저장 여부
        @Schema(description = "사용 완료 일시", example = "2026-05-12T16:35:00")
        LocalDateTime usedAt, // 사용 완료 일시
        @Schema(description = "만료 일시", example = "2026-05-12T16:30:00")
        LocalDateTime expiresAt // 만료 일시
) {
}
