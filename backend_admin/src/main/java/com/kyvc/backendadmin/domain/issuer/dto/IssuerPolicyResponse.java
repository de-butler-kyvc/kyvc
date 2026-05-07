package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Issuer 정책 상세 응답 DTO입니다. */
@Schema(description = "Issuer 정책 상세 응답")
public record IssuerPolicyResponse(
        /** Issuer 정책 ID */
        @Schema(description = "Issuer 정책 ID", example = "1")
        Long policyId,
        /** Issuer DID */
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid,
        /** Issuer 이름 */
        @Schema(description = "Issuer 이름", example = "KYvC Platform Issuer")
        String issuerName,
        /** 정책 유형 */
        @Schema(description = "정책 유형", example = "WHITELIST")
        String policyType,
        /** Credential 유형 목록 */
        @Schema(description = "Credential 유형 목록")
        List<String> credentialTypes,
        /** 정책 상태 */
        @Schema(description = "정책 상태", example = "ACTIVE")
        String status,
        /** 정책 사유 */
        @Schema(description = "정책 사유")
        String reason,
        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
