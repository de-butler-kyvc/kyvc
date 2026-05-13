package com.kyvc.backend.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Issuer 정책 응답
 *
 * @param issuerPolicyId Issuer 정책 ID
 * @param issuerDid Issuer DID
 * @param issuerName Issuer 이름
 * @param issuerPolicyType Issuer 정책 유형 코드
 * @param credentialTypeCode Credential 유형 코드
 * @param issuerPolicyStatus Issuer 정책 상태 코드
 * @param reason 정책 사유
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 */
@Schema(description = "Issuer 정책 응답")
public record IssuerPolicyResponse(
        @Schema(description = "Issuer 정책 ID", example = "1")
        Long issuerPolicyId, // Issuer 정책 ID
        @Schema(description = "Issuer DID", example = "did:example:issuer-1")
        String issuerDid, // Issuer DID
        @Schema(description = "Issuer 이름", example = "KYVC Trusted Issuer")
        String issuerName, // Issuer 이름
        @Schema(description = "Issuer 정책 유형 코드", example = "WHITELIST")
        String issuerPolicyType, // Issuer 정책 유형 코드
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL", nullable = true)
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "Issuer 정책 상태 코드", example = "ACTIVE")
        String issuerPolicyStatus, // Issuer 정책 상태 코드
        @Schema(description = "정책 사유", example = "신뢰 Issuer 유지")
        String reason, // 정책 사유
        @Schema(description = "생성 일시", example = "2026-05-05T10:30:00")
        LocalDateTime createdAt, // 생성 일시
        @Schema(description = "수정 일시", example = "2026-05-05T11:00:00")
        LocalDateTime updatedAt // 수정 일시
) {
}
