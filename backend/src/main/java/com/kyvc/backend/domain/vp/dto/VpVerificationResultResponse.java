package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VP 검증 결과 응답
 *
 * @param signatureValid 서명 유효 여부
 * @param issuerTrusted Issuer 신뢰 여부
 * @param credentialStatus Credential 상태
 * @param replayDetected Replay 탐지 여부
 */
@Schema(description = "VP 검증 결과 응답")
public record VpVerificationResultResponse(
        @Schema(description = "서명 유효 여부", example = "true")
        boolean signatureValid, // 서명 유효 여부
        @Schema(description = "Issuer 신뢰 여부", example = "true")
        boolean issuerTrusted, // Issuer 신뢰 여부
        @Schema(description = "Credential 상태", example = "VALID")
        String credentialStatus, // Credential 상태
        @Schema(description = "Replay 탐지 여부", example = "false")
        boolean replayDetected // Replay 탐지 여부
) {
}
