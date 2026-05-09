package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 개발/E2E 테스트용 KYC 임시 승인 응답
 *
 * @param kycId KYC 신청 ID
 * @param kycStatusCode KYC 상태 코드
 * @param credentialIssued VC 발급 요청 여부
 * @param credentialId Credential ID
 * @param credentialStatusCode Credential 상태 코드
 * @param message 처리 메시지
 */
@Schema(description = "개발/E2E 테스트용 KYC 임시 승인 응답")
public record DevKycApproveResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태 코드", example = "APPROVED")
        String kycStatusCode, // KYC 상태 코드
        @Schema(description = "VC 발급 요청 여부", example = "true")
        boolean credentialIssued, // VC 발급 요청 여부
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "Credential 상태 코드", example = "VALID")
        String credentialStatusCode, // Credential 상태 코드
        @Schema(description = "처리 메시지", example = "개발/E2E 테스트용 KYC 임시 승인 및 VC 발급 요청이 완료되었습니다.")
        String message // 처리 메시지
) {
}
