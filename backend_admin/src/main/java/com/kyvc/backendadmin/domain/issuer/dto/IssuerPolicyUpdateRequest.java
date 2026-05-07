package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Issuer 정책 수정 요청 DTO입니다. */
@Schema(description = "Issuer 정책 수정 요청")
public record IssuerPolicyUpdateRequest(
        /** Issuer 이름 */
        @Schema(description = "Issuer 이름", example = "KYvC Platform Issuer")
        String issuerName,
        /** Credential 유형 목록 */
        @Schema(description = "Credential 유형 목록", example = "[\"KYC_CREDENTIAL\"]")
        List<String> credentialTypes,
        /** 정책 상태 */
        @Schema(description = "정책 상태", example = "ACTIVE")
        String status,
        /** 정책 사유 */
        @Schema(description = "정책 사유", example = "정책 사유 수정")
        String reason,
        /** MFA 세션 토큰 */
        @Schema(description = "MFA 세션 토큰")
        String mfaToken
) {
}
