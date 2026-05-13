package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 금융사 방문 KYC QR 상태 응답
 *
 * @param kycId KYC 신청 ID
 * @param credentialId Credential ID
 * @param qrStatus QR 상태 코드
 * @param credentialStatus Credential 상태 코드
 * @param expiresAt 만료 일시
 * @param usedYn 사용 여부
 * @param walletSavedYn Wallet 저장 여부
 */
@Schema(description = "금융사 방문 KYC QR 상태 응답")
public record FinanceKycQrStatusResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "Credential ID", example = "1", nullable = true)
        Long credentialId, // Credential ID
        @Schema(description = "QR 상태 코드", example = "ACTIVE")
        String qrStatus, // QR 상태 코드
        @Schema(description = "Credential 상태 코드", example = "VALID", nullable = true)
        String credentialStatus, // Credential 상태 코드
        @Schema(description = "만료 일시", example = "2026-05-11T10:10:00", nullable = true)
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "사용 여부", example = "N")
        String usedYn, // 사용 여부
        @Schema(description = "Wallet 저장 여부", example = "N")
        String walletSavedYn // Wallet 저장 여부
) {
}
