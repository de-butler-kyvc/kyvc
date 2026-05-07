package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Issuer 발급 설정 상세 응답 DTO입니다. */
@Schema(description = "Issuer 발급 설정 상세 응답")
public record IssuerConfigDetailResponse(
        /** Issuer 발급 설정 ID */
        @Schema(description = "Issuer 발급 설정 ID", example = "1")
        Long issuerConfigId,
        /** Issuer DID */
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid,
        /** Issuer 이름 */
        @Schema(description = "Issuer 이름", example = "KYvC Platform Issuer")
        String issuerName,
        /** Issuer 유형 */
        @Schema(description = "Issuer 유형", example = "PLATFORM")
        String issuerType,
        /** Credential 유형 */
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType,
        /** 기본 Issuer 여부 */
        @Schema(description = "기본 Issuer 여부(Y/N)", example = "Y")
        String defaultYn,
        /** 설정 상태 */
        @Schema(description = "설정 상태", example = "ACTIVE")
        String status,
        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
