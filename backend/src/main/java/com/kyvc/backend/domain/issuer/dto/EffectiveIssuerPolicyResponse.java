package com.kyvc.backend.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유효 Issuer 정책 응답
 *
 * @param credentialTypeCode Credential 유형 코드
 * @param trustedIssuers 신뢰 Issuer DID 목록
 * @param policyVersion 정책 버전
 * @param effectiveAt 유효 기준 일시
 * @param policies 유효 Issuer 정책 목록
 * @param evaluatedAt 정책 평가 일시
 */
@Schema(description = "유효 Issuer 정책 응답")
public record EffectiveIssuerPolicyResponse(
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode, // Credential 유형 코드
        @Schema(description = "신뢰 Issuer DID 목록")
        List<String> trustedIssuers, // 신뢰 Issuer DID 목록
        @Schema(description = "정책 버전", example = "v1")
        String policyVersion, // 정책 버전
        @Schema(description = "유효 기준 일시", example = "2026-05-11T10:00:00")
        LocalDateTime effectiveAt, // 유효 기준 일시
        @Schema(description = "유효 Issuer 정책 목록")
        List<IssuerPolicyResponse> policies, // 유효 Issuer 정책 목록
        @Schema(description = "정책 평가 일시", example = "2026-05-11T10:00:00")
        LocalDateTime evaluatedAt // 정책 평가 일시
) {
    public EffectiveIssuerPolicyResponse(
            List<IssuerPolicyResponse> policies, // 유효 Issuer 정책 목록
            LocalDateTime evaluatedAt // 정책 평가 일시
    ) {
        this(null, List.of(), null, evaluatedAt, policies, evaluatedAt);
    }
}
