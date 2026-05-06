package com.kyvc.backendadmin.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 MFA challenge 생성 요청 DTO입니다.
 */
@Schema(description = "관리자 MFA challenge 생성 요청")
public record AdminMfaChallengeRequest(
        /** MFA 채널입니다. 현재 EMAIL만 지원합니다. */
        @Schema(description = "MFA 채널입니다. 현재 EMAIL만 지원합니다.", example = "EMAIL", allowableValues = "EMAIL")
        @NotBlank String channel,

        /** MFA 목적입니다. 예: LOGIN, KYC_APPROVE, KYC_REJECT. */
        @Schema(description = "MFA 목적입니다.", example = "KYC_APPROVE")
        @NotBlank String purpose
) {
}
