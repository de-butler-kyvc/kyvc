package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 웹 VP 로그인 상태 응답
 *
 * @param requestId VP 로그인 요청 ID
 * @param status VP 검증 상태
 * @param canComplete PC 로그인 완료 가능 여부
 * @param expiresAt KST offset 포함 만료 일시
 */
@Schema(description = "웹 VP 로그인 상태 응답")
public record WebVpLoginStatusResponse(
        @Schema(description = "VP 로그인 요청 ID", example = "vp-login-req-001")
        String requestId, // VP 로그인 요청 ID
        @Schema(description = "VP 검증 상태", example = "REQUESTED")
        String status, // VP 검증 상태
        @Schema(description = "PC 로그인 완료 가능 여부", example = "false")
        boolean canComplete, // PC 로그인 완료 가능 여부
        @Schema(description = "KST offset 포함 만료 일시", example = "2026-05-13T20:55:53+09:00")
        String expiresAt // KST offset 포함 만료 일시
) {
}
