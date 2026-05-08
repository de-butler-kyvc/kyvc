package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 제출 가능 Credential 응답
 *
 * @param credentialId Credential ID
 * @param credentialTypeCode Credential 유형 코드
 * @param issuerDid 발급자 DID
 * @param issuedAt 발급 일시
 * @param expiresAt 만료 일시
 * @param matchReason 제출 가능 판단 사유
 */
@Schema(description = "제출 가능 Credential 응답")
public record EligibleCredentialResponse(
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "발급자 DID", example = "did:kyvc:issuer")
        String issuerDid, // 발급자 DID
        @Schema(description = "발급 일시", example = "2026-05-07T12:30:00")
        LocalDateTime issuedAt, // 발급 일시
        @Schema(description = "만료 일시", example = "2027-05-07T12:30:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "제출 가능 판단 사유", example = "동일 법인의 Wallet 저장 VALID Credential입니다.")
        String matchReason // 제출 가능 판단 사유
) {
}
