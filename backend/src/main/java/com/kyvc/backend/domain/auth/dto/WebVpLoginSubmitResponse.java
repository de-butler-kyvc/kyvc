package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 웹 VP 로그인 모바일 제출 응답
 *
 * @param requestId VP 로그인 요청 ID
 * @param status VP 검증 상태
 * @param verified Core 검증 성공 여부
 */
@Schema(description = "웹 VP 로그인 모바일 제출 응답")
public record WebVpLoginSubmitResponse(
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "VP 검증 상태", example = "VALID")
        String status, // VP 검증 상태
        @Schema(description = "Core 검증 성공 여부", example = "true")
        boolean verified // Core 검증 성공 여부
) {
}
