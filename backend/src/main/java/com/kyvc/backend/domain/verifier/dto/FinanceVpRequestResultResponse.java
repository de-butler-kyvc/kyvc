package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 금융사 VP 요청 결과 응답
 *
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param verifiedAt 검증 일시
 * @param corporateRegistrationNo 법인등록번호
 * @param representativeName 대표자명
 * @param kycStatus KYC 상태
 * @param credentialStatus Credential 상태
 * @param credentialIssuedAt Credential 발급 일시
 * @param credentialExpiresAt Credential 만료 일시
 */
@Schema(description = "금융사 VP 요청 결과 응답")
public record FinanceVpRequestResultResponse(
        @Schema(description = "법인명", example = "주식회사 KYVC")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "검증 일시", example = "2026-05-11T10:00:00")
        LocalDateTime verifiedAt, // 검증 일시
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "대표자명", example = "김대표")
        String representativeName, // 대표자명
        @Schema(description = "KYC 상태", example = "APPROVED")
        String kycStatus, // KYC 상태
        @Schema(description = "Credential 상태", example = "VALID")
        String credentialStatus, // Credential 상태
        @Schema(description = "Credential 발급 일시", example = "2026-05-16T14:14:21")
        LocalDateTime credentialIssuedAt, // Credential 발급 일시
        @Schema(description = "Credential 만료 일시", example = "2026-12-31T23:59:59")
        LocalDateTime credentialExpiresAt // Credential 만료 일시
) {
    public FinanceVpRequestResultResponse(
            String corporateName, // 법인명
            String businessRegistrationNo, // 사업자등록번호
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this(corporateName, businessRegistrationNo, verifiedAt, null, null, null, null, null, null);
    }
}
