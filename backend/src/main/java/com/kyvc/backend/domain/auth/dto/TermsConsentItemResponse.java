package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 약관 동의 항목 응답
 *
 * @param termsCode 약관 코드
 * @param termsVersion 약관 버전
 * @param required 필수 약관 여부
 * @param agreed 동의 여부
 * @param agreedAt 동의 일시
 */
@Schema(description = "약관별 동의 상태 응답")
public record TermsConsentItemResponse(
        @Schema(description = "약관 코드", example = "TERMS_OF_SERVICE")
        String termsCode, // 약관 코드
        @Schema(description = "약관 버전", example = "1.0")
        String termsVersion, // 약관 버전
        @Schema(description = "필수 약관 여부", example = "true")
        boolean required, // 필수 약관 여부
        @Schema(description = "동의 여부", example = "true")
        boolean agreed, // 동의 여부
        @Schema(description = "동의 일시", example = "2026-05-04T12:30:00")
        LocalDateTime agreedAt // 동의 일시
) {
}
