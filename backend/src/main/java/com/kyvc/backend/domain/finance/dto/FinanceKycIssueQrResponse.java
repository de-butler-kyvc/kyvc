package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 금융사 방문 KYC QR 발급 응답
 *
 * @param kycId KYC 신청 ID
 * @param credentialId Credential ID
 * @param qrPayload QR payload
 * @param expiresAt 만료 일시
 * @param qrStatus QR 상태 코드
 */
@Schema(description = "금융사 방문 KYC QR 발급 응답")
public record FinanceKycIssueQrResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "QR payload")
        String qrPayload, // QR payload
        @Schema(description = "만료 일시", example = "2026-05-11T10:10:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "QR 상태 코드", example = "ACTIVE")
        String qrStatus // QR 상태 코드
) {
}
