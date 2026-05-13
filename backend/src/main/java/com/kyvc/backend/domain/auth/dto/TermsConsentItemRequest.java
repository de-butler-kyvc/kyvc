package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 약관 동의 항목 저장 요청
 *
 * @param termsCode 약관 코드
 * @param termsVersion 약관 버전
 * @param required 필수 약관 여부
 * @param agreed 동의 여부
 */
@Schema(description = "약관 동의 항목 저장 요청")
public record TermsConsentItemRequest(
        @Schema(description = "약관 코드", example = "TERMS_OF_SERVICE")
        @NotBlank(message = "약관 코드는 필수입니다.")
        String termsCode, // 약관 코드
        @Schema(description = "약관 버전", example = "1.0")
        @NotBlank(message = "약관 버전은 필수입니다.")
        String termsVersion, // 약관 버전
        @Schema(description = "필수 약관 여부", example = "true")
        @NotNull(message = "필수 약관 여부는 필수입니다.")
        Boolean required, // 필수 약관 여부
        @Schema(description = "동의 여부", example = "true")
        @NotNull(message = "동의 여부는 필수입니다.")
        Boolean agreed // 동의 여부
) {
}
