package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 개발/E2E 테스트용 KYC 임시 승인 요청
 *
 * @param adminId 임시 승인 처리자 ID
 * @param reason 임시 승인 사유
 * @param issueCredential 승인 후 VC 발급 요청 여부
 */
@Schema(description = "개발/E2E 테스트용 KYC 임시 승인 요청")
public record DevKycApproveRequest(
        @Schema(description = "임시 승인 처리자 ID", example = "1")
        Long adminId, // 임시 승인 처리자 ID
        @Schema(description = "임시 승인 사유", example = "E2E 테스트용 임시 승인")
        String reason, // 임시 승인 사유
        @Schema(description = "승인 후 VC 발급 요청 여부", example = "true")
        Boolean issueCredential // 승인 후 VC 발급 요청 여부
) {
    public boolean shouldIssueCredential() {
        return issueCredential == null || Boolean.TRUE.equals(issueCredential);
    }
}
