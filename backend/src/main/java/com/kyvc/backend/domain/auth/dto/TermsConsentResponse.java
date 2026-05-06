package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 약관 동의 상태 응답
 *
 * @param userId 사용자 ID
 * @param allRequiredAgreed 필수 약관 전체 동의 여부
 * @param consents 약관별 동의 상태 목록
 */
@Schema(description = "약관 동의 상태 응답")
public record TermsConsentResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "필수 약관 전체 동의 여부", example = "true")
        boolean allRequiredAgreed, // 필수 약관 전체 동의 여부
        @Schema(description = "약관별 동의 상태 목록")
        List<TermsConsentItemResponse> consents // 약관별 동의 상태 목록
) {

    public TermsConsentResponse {
        consents = consents == null ? List.of() : List.copyOf(consents);
    }
}
