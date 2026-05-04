package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 약관 동의 저장 요청
 *
 * @param consents 약관 동의 목록
 */
@Schema(description = "약관 동의 저장 요청")
public record TermsConsentRequest(
        @Schema(description = "저장할 약관 동의 목록")
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        List<@Valid TermsConsentItemRequest> consents // 약관 동의 목록
) {
}
