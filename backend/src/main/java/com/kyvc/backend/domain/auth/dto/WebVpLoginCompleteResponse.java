package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 웹 VP 로그인 완료 응답
 *
 * @param userId 사용자 ID
 * @param corporateId 법인 ID
 * @param email 로그인 이메일
 * @param name 사용자명
 */
@Schema(description = "웹 VP 로그인 완료 응답")
public record WebVpLoginCompleteResponse(
        @Schema(description = "사용자 ID", example = "10")
        Long userId, // 사용자 ID
        @Schema(description = "법인 ID", example = "20")
        Long corporateId, // 법인 ID
        @Schema(description = "로그인 이메일", example = "user@example.com")
        String email, // 로그인 이메일
        @Schema(description = "사용자명", example = "홍길동")
        String name // 사용자명
) {
}
