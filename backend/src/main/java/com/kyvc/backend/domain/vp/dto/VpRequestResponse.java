package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * VP 요청 응답
 *
 * @param requestId VP 요청 ID
 * @param requesterName 요청자명
 * @param purpose 제출 목적
 * @param requiredClaims 요구 Claim JSON
 * @param requiredDisclosures Core 필수 disclosure 목록
 * @param documentRules Core 문서 증빙 규칙 목록
 * @param presentationDefinition Core Presentation Definition
 * @param challenge VP 요청 challenge
 * @param nonce VP 요청 nonce
 * @param expiresAt 요청 만료 일시
 * @param expired 요청 만료 여부
 * @param submitted 제출 완료 여부
 * @param status VP 요청 상태
 * @param result VP 검증 결과
 * @param verifiedAt 검증 일시
 */
@Schema(description = "VP 요청 응답")
public record VpRequestResponse(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "요청자명", example = "KYVC Verifier")
        String requesterName, // 요청자명
        @Schema(description = "제출 목적", example = "KYC 제출 증빙")
        String purpose, // 제출 목적
        @Schema(description = "요구 Claim JSON", example = "[\"legalEntity.type\",\"representative.name\"]")
        String requiredClaims, // 요구 Claim JSON
        @Schema(description = "Core 필수 disclosure 목록")
        List<String> requiredDisclosures, // Core 필수 disclosure 목록
        @Schema(description = "Core 문서 증빙 규칙 목록")
        List<Map<String, Object>> documentRules, // Core 문서 증빙 규칙 목록
        @Schema(description = "Core Presentation Definition")
        Map<String, Object> presentationDefinition, // Core Presentation Definition
        @Schema(description = "VP 요청 challenge", example = "challenge-001")
        String challenge, // VP 요청 challenge
        @Schema(description = "VP 요청 nonce", example = "nonce-001")
        String nonce, // VP 요청 nonce
        @Schema(description = "요청 만료 일시", example = "2026-05-07T23:59:59")
        LocalDateTime expiresAt, // 요청 만료 일시
        @Schema(description = "요청 만료 여부", example = "false")
        boolean expired, // 요청 만료 여부
        @Schema(description = "제출 완료 여부", example = "false")
        boolean submitted, // 제출 완료 여부
        @Schema(description = "VP 요청 상태", example = "REQUESTED")
        String status, // VP 요청 상태
        @Schema(description = "VP 검증 결과")
        VpVerificationResultResponse result, // VP 검증 결과
        @Schema(description = "검증 일시", example = "2026-05-07T16:05:00")
        LocalDateTime verifiedAt // 검증 일시
) {
}
