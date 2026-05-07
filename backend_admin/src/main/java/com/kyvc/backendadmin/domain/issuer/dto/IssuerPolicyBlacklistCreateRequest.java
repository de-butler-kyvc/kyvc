package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Issuer 블랙리스트 등록 요청 DTO입니다. */
@Schema(description = "Issuer 블랙리스트 등록 요청")
public record IssuerPolicyBlacklistCreateRequest(
        /** Issuer DID */
        @NotBlank @Schema(description = "Issuer DID", example = "did:xrpl:1:rBadIssuer")
        String issuerDid,
        /** Issuer 이름 */
        @NotBlank @Schema(description = "Issuer 이름", example = "Untrusted Issuer")
        String issuerName,
        /** 차단 사유 코드 */
        @NotBlank @Schema(description = "차단 사유 코드", example = "FRAUD_SUSPECTED")
        String reasonCode,
        /** 차단 상세 사유 */
        @Schema(description = "차단 상세 사유", example = "위조 VC 발급 의심")
        String reason,
        /** MFA 세션 토큰 */
        @NotBlank @Schema(description = "MFA 세션 토큰")
        String mfaToken
) {
}
