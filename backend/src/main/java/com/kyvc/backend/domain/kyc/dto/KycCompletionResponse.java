package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 완료 화면 응답
 *
 * @param kycId KYC 요청 ID
 * @param corporateId 법인 ID
 * @param corporateName 법인명
 * @param status KYC 상태
 * @param approvedAt 승인 일시
 * @param credentialIssued Credential 발급 완료 여부
 * @param credentialId Credential ID
 * @param nextActionCode 다음 행동 코드
 * @param message 안내 메시지
 */
@Schema(description = "KYC 완료 화면 응답")
public record KycCompletionResponse(
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "법인명", example = "주식회사 KYVC")
        String corporateName, // 법인명
        @Schema(description = "KYC 상태", example = "APPROVED")
        String status, // KYC 상태
        @Schema(description = "승인 일시", example = "2026-05-05T14:00:00")
        LocalDateTime approvedAt, // 승인 일시
        @Schema(description = "Credential 발급 완료 여부", example = "false")
        Boolean credentialIssued, // Credential 발급 완료 여부
        @Schema(description = "Credential ID", example = "10")
        Long credentialId, // Credential ID
        @Schema(description = "다음 행동 코드", example = "ISSUE_CREDENTIAL")
        String nextActionCode, // 다음 행동 코드
        @Schema(description = "안내 메시지", example = "KYC 심사가 완료되었습니다. VC를 발급할 수 있습니다.")
        String message // 안내 메시지
) {
}
