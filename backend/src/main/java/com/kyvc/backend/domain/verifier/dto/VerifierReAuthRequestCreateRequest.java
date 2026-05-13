package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Verifier 기업 재인증 요청 생성 요청
 *
 * @param corporateId 재인증 대상 법인 ID
 * @param reason 재인증 요청 사유
 * @param requestedClaims 요청 Claim 목록
 * @param resultNotifyUrl 외부 Verifier 결과 통지 URL
 */
@Schema(description = "Verifier 기업 재인증 요청 생성 요청")
public record VerifierReAuthRequestCreateRequest(
        @NotNull
        @Schema(description = "재인증 대상 법인 ID", example = "1")
        Long corporateId, // 재인증 대상 법인 ID
        @NotBlank
        @Schema(description = "재인증 요청 사유", example = "정기 재인증")
        String reason, // 재인증 요청 사유
        @Schema(description = "요청 Claim 목록")
        List<String> requestedClaims, // 요청 Claim 목록
        @Schema(description = "외부 Verifier 결과 통지 URL", example = "https://verifier.example.com/kyvc/result")
        String resultNotifyUrl // 외부 Verifier 결과 통지 URL
) {
}
