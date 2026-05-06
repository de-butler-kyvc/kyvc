package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그아웃 응답
 *
 * @param loggedOut 로그아웃 완료 여부
 */
@Schema(description = "로그아웃 응답")
public record LogoutResponse(
        @Schema(description = "로그아웃 완료 여부", example = "true")
        boolean loggedOut // 로그아웃 완료 여부
) {

    /**
     * @return 로그아웃 완료 여부
     */
    public boolean loggedOut() {
        return loggedOut;
    }
}
