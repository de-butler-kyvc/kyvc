package com.kyvc.backendadmin.domain.issuer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Issuer 정책 승인요청 DTO */
@Schema(description = "Issuer 정책 승인요청")
public record IssuerPolicySubmitApprovalRequest(
        /** 승인요청 의견 */
        @Schema(description = "승인요청 의견", example = "정책 적용 검토 요청")
        String comment
) {
}
