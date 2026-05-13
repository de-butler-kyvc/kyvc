package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 역할 선택 응답
 *
 * @param roleCode 선택 역할 코드
 * @param selected 선택 완료 여부
 */
@Schema(description = "역할 선택 응답")
public record AuthRoleSelectResponse(
        @Schema(description = "선택 역할 코드", example = "CORPORATE_USER")
        String roleCode, // 선택 역할 코드
        @Schema(description = "선택 완료 여부", example = "true")
        boolean selected // 선택 완료 여부
) {
}
