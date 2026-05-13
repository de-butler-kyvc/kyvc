package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VP 검증 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param vpVerificationId VP 검증 ID
 * @param credentialId Credential ID
 * @param corporateId 법인 ID
 * @param requestNonce 요청 nonce
 * @param challenge 요청 challenge
 * @param purpose 검증 목적
 * @param aud 검증 aud
 * @param requiredClaimsJson 요청 claim 조건 JSON
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core VP 검증 요청")
public record CoreVpVerificationRequest(
        @Schema(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "VP 검증 ID", example = "21")
        Long vpVerificationId, // VP 검증 ID
        @Schema(description = "Credential ID", example = "11")
        Long credentialId, // Credential ID
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "요청 nonce", example = "nonce-20260506-001")
        String requestNonce, // 요청 nonce
        @Schema(description = "요청 challenge", example = "challenge-20260506-001")
        String challenge, // 요청 challenge
        @Schema(description = "검증 목적", example = "KYC 제출 증명")
        String purpose, // 검증 목적
        @Schema(description = "검증 aud", example = "https://dev-api-kyvc.khuoo.synology.me")
        String aud, // 검증 aud
        @Schema(description = "요청 claim 조건 JSON")
        String requiredClaimsJson, // 요청 claim 조건 JSON
        @Schema(description = "요청 시각", example = "2026-05-06T10:20:00")
        LocalDateTime requestedAt // 요청 시각
) {
}
