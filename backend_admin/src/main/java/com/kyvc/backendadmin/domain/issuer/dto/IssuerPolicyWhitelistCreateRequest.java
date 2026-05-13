package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Issuer 화이트리스트 등록 요청 DTO입니다. */
@Schema(description = "Issuer 화이트리스트 등록 요청")
public record IssuerPolicyWhitelistCreateRequest(
        /** Issuer DID */
        @NotBlank @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        String issuerDid,
        /** Issuer 이름 */
        @NotBlank @Schema(description = "Issuer 이름", example = "KYvC Platform Issuer")
        String issuerName,
        /** Credential 유형 목록 */
        @Schema(description = "Credential 유형 목록", example = "[\"KYC_CREDENTIAL\"]")
        List<String> credentialTypes,
        /** 등록 사유 */
        @Schema(description = "등록 사유", example = "신뢰 가능한 발급기관")
        String reason,
        /** MFA 세션 토큰 */
        @NotBlank @Schema(description = "MFA 세션 토큰")
        String mfaToken
) {
}
