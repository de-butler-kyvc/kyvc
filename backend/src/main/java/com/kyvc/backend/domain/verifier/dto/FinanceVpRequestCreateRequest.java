package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 금융사 VP 요청 생성 요청
 *
 * @param purpose VP 요청 목적
 * @param requestedClaims 요청 Claim 목록
 * @param expiresInSeconds QR 유효 초
 */
@Schema(description = "금융사 VP 요청 생성 요청")
public record FinanceVpRequestCreateRequest(
        @NotBlank
        @Schema(description = "VP 요청 목적", example = "ACCOUNT_OPENING")
        String purpose, // VP 요청 목적
        @NotEmpty
        @Schema(description = "요청 Claim 목록", example = "[\"corporateName\",\"businessRegistrationNo\"]")
        List<String> requestedClaims, // 요청 Claim 목록
        @Positive
        @Schema(description = "QR 유효 초", example = "600")
        Long expiresInSeconds // QR 유효 초
) {
}
