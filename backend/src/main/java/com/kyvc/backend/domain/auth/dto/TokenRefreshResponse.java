package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 재발급 응답
 *
 * @param refreshed 재발급 완료 여부
 */
@Schema(description = "토큰 재발급 응답")
public record TokenRefreshResponse(
        @Schema(description = "토큰 재발급 완료 여부", example = "true")
        boolean refreshed // 재발급 완료 여부
) {

    /**
     * @return 재발급 완료 여부
     */
    public boolean refreshed() {
        return refreshed;
    }
}
