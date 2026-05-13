package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VC 발급 안내 응답
 *
 * @param corporateId 법인 ID
 * @param latestKycId 최신 KYC 요청 ID
 * @param kycStatus KYC 상태
 * @param credentialIssued Credential 발급 완료 여부
 * @param credentialStatus Credential 상태
 * @param issueAvailable VC 발급 가능 여부
 * @param nextActionCode 다음 행동 코드
 * @param guideTitle 안내 제목
 * @param guideMessage 안내 메시지
 */
@Schema(description = "VC 발급 안내 응답")
public record CredentialIssueGuideResponse(
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "최신 KYC 요청 ID", example = "10")
        Long latestKycId, // 최신 KYC 요청 ID
        @Schema(description = "KYC 상태", example = "APPROVED")
        String kycStatus, // KYC 상태
        @Schema(description = "Credential 발급 완료 여부", example = "false")
        Boolean credentialIssued, // Credential 발급 완료 여부
        @Schema(description = "Credential 상태", example = "VALID")
        String credentialStatus, // Credential 상태
        @Schema(description = "VC 발급 가능 여부", example = "true")
        Boolean issueAvailable, // VC 발급 가능 여부
        @Schema(description = "다음 행동 코드", example = "ISSUE_CREDENTIAL")
        String nextActionCode, // 다음 행동 코드
        @Schema(description = "안내 제목", example = "VC 발급이 가능합니다.")
        String guideTitle, // 안내 제목
        @Schema(description = "안내 메시지", example = "KYC 심사가 승인되었습니다. VC 발급을 진행할 수 있습니다.")
        String guideMessage // 안내 메시지
) {
}
